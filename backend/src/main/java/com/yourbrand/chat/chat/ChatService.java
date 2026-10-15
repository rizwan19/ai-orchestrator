package com.yourbrand.chat.chat;

import com.yourbrand.chat.chat.ChatDtos.ConversationResponse;
import com.yourbrand.chat.chat.ChatDtos.MessageResponse;
import com.yourbrand.chat.config.ChatProperties;
import com.yourbrand.chat.openrouter.ChatCompletionClient;
import com.yourbrand.chat.openrouter.ChatCompletionClient.ChatMessage;
import com.yourbrand.chat.openrouter.ChatCompletionClient.Completion;
import com.yourbrand.chat.openrouter.OpenRouterException;
import com.yourbrand.chat.usage.BudgetService;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Limit;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Service
class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    /** Everything the streaming phase needs, resolved while the request is still synchronous. */
    record PreparedReply(UUID userId, UUID conversationId, String model, List<ChatMessage> history) {}

    /** The browser closed the stream (navigated away or pressed Stop). */
    private static final class ClientGoneException extends RuntimeException {}

    private final ConversationRepository conversations;
    private final MessageRepository messages;
    private final BudgetService budget;
    private final ChatCompletionClient client;
    private final ChatProperties properties;
    private final TransactionTemplate transaction;

    ChatService(
            ConversationRepository conversations,
            MessageRepository messages,
            BudgetService budget,
            ChatCompletionClient client,
            ChatProperties properties,
            TransactionTemplate transaction) {
        this.conversations = conversations;
        this.messages = messages;
        this.budget = budget;
        this.client = client;
        this.properties = properties;
        this.transaction = transaction;
    }

    List<ConversationResponse> list(UUID userId) {
        return conversations.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(ConversationResponse::of)
                .toList();
    }

    ConversationResponse create(UUID userId, String model) {
        String chosen = model == null ? properties.defaultModel().id() : supportedModel(model);
        return ConversationResponse.of(conversations.save(new Conversation(userId, chosen)));
    }

    @Transactional
    ConversationResponse rename(UUID userId, UUID conversationId, String title) {
        var conversation = owned(userId, conversationId);
        conversation.rename(title.strip());
        return ConversationResponse.of(conversation);
    }

    @Transactional
    void delete(UUID userId, UUID conversationId) {
        conversations.delete(owned(userId, conversationId));
    }

    List<MessageResponse> messages(UUID userId, UUID conversationId) {
        owned(userId, conversationId);
        return messages.findByConversationIdOrderByCreatedAtAsc(conversationId).stream()
                .map(MessageResponse::of)
                .toList();
    }

    @Transactional
    PreparedReply prepare(UUID userId, UUID conversationId, String content, String model) {
        var conversation = owned(userId, conversationId);
        supportedModel(model);
        if (content.length() > properties.maxMessageLength()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Messages are limited to %d characters.".formatted(properties.maxMessageLength()));
        }
        if (budget.today(userId).exhausted()) {
            throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS, "You've reached today's usage limit. It resets at midnight.");
        }

        messages.save(Message.user(conversationId, content));
        conversation.recordUserMessage(model, content);

        var history = messages.findByConversationIdOrderByCreatedAtDesc(conversationId, Limit.of(properties.historyLimit()))
                .reversed()
                .stream()
                .map(message -> new ChatMessage(message.getRole().value(), message.getContent()))
                .toList();
        return new PreparedReply(userId, conversationId, model, history);
    }

    /** Streams the reply as SSE events: {@code delta} {text}, then {@code done} {message, usage} or {@code error} {message}. */
    void streamReply(PreparedReply reply, SseEmitter emitter) {
        var partial = new StringBuilder();
        try {
            Completion completion = client.stream(reply.model(), reply.history(), delta -> {
                partial.append(delta);
                send(emitter, "delta", Map.of("text", delta));
            });
            Message saved = saveAssistant(reply, completion);
            send(emitter, "done", Map.of("message", MessageResponse.of(saved), "usage", budget.today(reply.userId())));
            emitter.complete();
        } catch (ClientGoneException e) {
            // Keep what was generated so far. Cost is unknown for an aborted stream, so it is not recorded.
            if (!partial.isEmpty()) {
                saveAssistant(reply, new Completion(partial.toString(), null, null, null));
            }
        } catch (OpenRouterException e) {
            log.warn("OpenRouter error {} for model {}: {}", e.status(), reply.model(), e.getMessage());
            fail(emitter, userFacingMessage(e));
        } catch (RuntimeException e) {
            log.error("Chat stream failed", e);
            fail(emitter, "Something went wrong while generating the reply.");
        }
    }

    private Message saveAssistant(PreparedReply reply, Completion completion) {
        return transaction.execute(status -> {
            var message = messages.save(Message.assistant(
                    reply.conversationId(),
                    reply.model(),
                    completion.content(),
                    completion.promptTokens(),
                    completion.completionTokens(),
                    completion.cost()));
            if (completion.cost() != null) {
                budget.record(reply.userId(), reply.model(), completion.cost());
            }
            return message;
        });
    }

    private Conversation owned(UUID userId, UUID conversationId) {
        return conversations.findByIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversation not found."));
    }

    private String supportedModel(String model) {
        if (!properties.supports(model)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This model is not available.");
        }
        return model;
    }

    /** Hides account-level problems (bad key, no credits) from end users. */
    private static String userFacingMessage(OpenRouterException e) {
        return switch (e.status()) {
            case 401, 402, 403 -> "The AI service is temporarily unavailable. Please try again later.";
            case 429 -> "The model is busy right now. Please try again in a moment.";
            default -> e.getMessage();
        };
    }

    private static void send(SseEmitter emitter, String event, Object data) {
        try {
            emitter.send(SseEmitter.event().name(event).data(data, MediaType.APPLICATION_JSON));
        } catch (IOException | IllegalStateException e) {
            throw new ClientGoneException();
        }
    }

    private static void fail(SseEmitter emitter, String message) {
        try {
            send(emitter, "error", Map.of("message", message));
            emitter.complete();
        } catch (ClientGoneException ignored) {
            // Nobody is listening any more.
        }
    }
}
