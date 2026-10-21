package com.yourbrand.chat.conversation.service;

import com.yourbrand.chat.common.exception.BadRequestException;
import com.yourbrand.chat.config.ChatProperties;
import com.yourbrand.chat.conversation.entity.Message;
import com.yourbrand.chat.conversation.service.model.PreparedReply;
import com.yourbrand.chat.conversation.service.model.ReplyListener;
import com.yourbrand.chat.conversation.service.model.ReplyListener.ClientGoneException;
import com.yourbrand.chat.integration.openrouter.ChatCompletionClient;
import com.yourbrand.chat.integration.openrouter.ChatCompletionClient.ChatMessage;
import com.yourbrand.chat.integration.openrouter.ChatCompletionClient.Completion;
import com.yourbrand.chat.integration.openrouter.OpenRouterException;
import com.yourbrand.chat.usage.service.UsageService;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/** Sends a user's message to the model and persists the reply, recording its cost against the daily budget. */
@Service
public class ChatReplyService {

    private static final Logger log = LoggerFactory.getLogger(ChatReplyService.class);

    private final ConversationService conversations;
    private final MessageService messages;
    private final ModelCatalogService models;
    private final UsageService usage;
    private final ChatCompletionClient client;
    private final ChatProperties properties;
    private final TransactionTemplate transaction;

    ChatReplyService(
            ConversationService conversations,
            MessageService messages,
            ModelCatalogService models,
            UsageService usage,
            ChatCompletionClient client,
            ChatProperties properties,
            TransactionTemplate transaction) {
        this.conversations = conversations;
        this.messages = messages;
        this.models = models;
        this.usage = usage;
        this.client = client;
        this.properties = properties;
        this.transaction = transaction;
    }

    /** Validates the request and saves the user's message; failures here surface as plain HTTP errors. */
    @Transactional
    public PreparedReply prepare(UUID userId, UUID conversationId, String content, String model) {
        var conversation = conversations.getOwned(userId, conversationId);
        models.requireSupported(model);
        if (content.length() > properties.maxMessageLength()) {
            throw new BadRequestException(
                    "Messages are limited to %d characters.".formatted(properties.maxMessageLength()));
        }
        usage.assertWithinBudget(userId);

        messages.saveUser(conversationId, content);
        conversation.recordUserMessage(model, content);

        var history = messages.recentHistory(conversationId).stream()
                .map(message -> new ChatMessage(message.getRole().value(), message.getContent()))
                .toList();
        return new PreparedReply(userId, conversationId, model, history);
    }

    /** Blocks until the reply finishes; progress and the outcome are reported to {@code listener}. */
    public void generate(PreparedReply reply, ReplyListener listener) {
        var partial = new StringBuilder();
        Message saved = null;
        try {
            Completion completion = client.stream(reply.model(), reply.history(), delta -> {
                partial.append(delta);
                listener.onDelta(delta);
            });
            saved = saveAssistant(reply, completion);
            listener.onComplete(saved, usage.today(reply.userId()));
        } catch (ClientGoneException e) {
            // Keep what was generated so far. Cost is unknown for an aborted stream, so it is not recorded.
            if (saved == null && !partial.isEmpty()) {
                saveAssistant(reply, new Completion(partial.toString(), null, null, null));
            }
        } catch (OpenRouterException e) {
            log.warn("OpenRouter error {} for model {}: {}", e.status(), reply.model(), e.getMessage());
            listener.onError(userFacingMessage(e));
        } catch (RuntimeException e) {
            log.error("Chat stream failed", e);
            listener.onError("Something went wrong while generating the reply.");
        }
    }

    private Message saveAssistant(PreparedReply reply, Completion completion) {
        return transaction.execute(status -> {
            var message = messages.saveAssistant(reply.conversationId(), reply.model(), completion);
            if (completion.cost() != null) {
                usage.record(reply.userId(), reply.model(), completion.cost());
            }
            return message;
        });
    }

    /** Hides account-level problems (bad key, no credits) from end users. */
    private static String userFacingMessage(OpenRouterException e) {
        return switch (e.status()) {
            case 401, 402, 403 -> "The AI service is temporarily unavailable. Please try again later.";
            case 429 -> "The model is busy right now. Please try again in a moment.";
            default -> e.getMessage();
        };
    }
}
