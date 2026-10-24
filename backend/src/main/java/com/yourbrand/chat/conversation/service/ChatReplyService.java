package com.yourbrand.chat.conversation.service;

import com.yourbrand.chat.common.concurrent.OperationCancelledException;
import com.yourbrand.chat.common.exception.BadRequestException;
import com.yourbrand.chat.common.exception.ContextBudgetException;
import com.yourbrand.chat.common.exception.DomainException;
import com.yourbrand.chat.config.ChatProperties;
import com.yourbrand.chat.config.ContextProperties;
import com.yourbrand.chat.conversation.entity.Message;
import com.yourbrand.chat.conversation.service.model.ReplyContext;
import com.yourbrand.chat.conversation.service.model.ReplyListener;
import com.yourbrand.chat.conversation.service.model.ReplyListener.ClientGoneException;
import com.yourbrand.chat.integration.openrouter.ChatCompletionClient;
import com.yourbrand.chat.integration.openrouter.ChatMessage;
import com.yourbrand.chat.integration.openrouter.Completion;
import com.yourbrand.chat.integration.openrouter.OpenRouterException;
import com.yourbrand.chat.usage.service.UsageService;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class ChatReplyService {

    private static final Logger log = LoggerFactory.getLogger(ChatReplyService.class);

    private final ConversationService conversationService;
    private final MessageService messageService;
    private final ModelCatalogService modelCatalogService;
    private final UsageService usageService;
    private final ChatCompletionClient client;
    private final ChatProperties properties;
    private final ContextProperties contextProperties;
    private final TokenEstimator tokenEstimator;
    private final ContextManagementService contextManagement;
    private final ActiveReplyGuard activeReplyGuard;
    private final TransactionTemplate transaction;

    public ReplyContext acceptUserMessage(UUID userId, UUID conversationId, String content, String model) {
        conversationService.getConversationById(userId, conversationId);
        modelCatalogService.requireSupported(model);
        if (content.length() > properties.maxMessageLength()) {
            throw new BadRequestException(
                    "Messages are limited to %d characters.".formatted(properties.maxMessageLength()));
        }
        if (tokenEstimator.estimatePromptToken(List.of(new ChatMessage("user", content))) > contextProperties.maxInputTokens()) {
            throw new ContextBudgetException("Your message is too long for the model's context. Please shorten it.");
        }
        usageService.assertWithinBudget(userId);

        var run = activeReplyGuard.acquire(conversationId);
        try {
            Message savedMessage = transaction.execute(status -> {
                var conversation = conversationService.lockOwned(userId, conversationId);
                var message = messageService.saveMessage(conversation, content);
                conversation.recordUserMessage(model, content);
                return message;
            });
            return new ReplyContext(userId, conversationId, model, savedMessage, run.runId());
        } catch (RuntimeException e) {
            activeReplyGuard.release(conversationId, run.runId());
            throw e;
        }
    }

    /** Stops the run: pending provider calls abort and nothing further is paid for. Safe to call repeatedly. */
    public void cancel(ReplyContext reply) {
        activeReplyGuard.cancel(reply.conversationId(), reply.runId());
    }

    /** Releases the guard of a run whose worker never started. */
    public void abandon(ReplyContext reply) {
        release(reply);
    }

    /** Blocks until the reply finishes; progress and the outcome are reported to {@code listener}. */
    public void generate(ReplyContext reply, ReplyListener listener) {
        var run = activeReplyGuard.require(reply.conversationId(), reply.runId());
        var cancellation = run.cancellation();
        var partial = new StringBuilder();
        Message saved = null;
        try {
            var prompt = contextManagement.prepare(reply, cancellation);

            cancellation.throwIfCancelled();
            usageService.assertWithinBudget(reply.userId());

            Completion completion = client.stream(reply.model(), prompt, delta -> {
                partial.append(delta);
                listener.onDelta(delta);
            }, cancellation);
            saved = saveAssistant(reply, completion);
            var today = usageService.today(reply.userId());
            // Nothing is written after this point, so the next message may start as soon as the client sees "done".
            release(reply);
            listener.onComplete(saved, today);
        } catch (ClientGoneException | OperationCancelledException e) {
            // Keep what was generated so far. Cost is unknown for an aborted stream, so it is not recorded.
            if (saved == null && !partial.isEmpty()) {
                saveAssistant(reply, new Completion(partial.toString(), null, null, null));
            }
        } catch (OpenRouterException e) {
            log.warn("OpenRouter error {} for model {}: {}", e.status(), reply.model(), e.getMessage());
            release(reply);
            listener.onError(userFacingMessage(e));
        } catch (DomainException e) {
            // Budget exhausted, context budget exceeded, or a concurrent summary conflict.
            release(reply);
            listener.onError(e.getMessage());
        } catch (RuntimeException e) {
            log.error("Chat stream failed", e);
            release(reply);
            listener.onError("Something went wrong while generating the reply.");
        } finally {
            release(reply);
        }
    }

    /** Idempotent: only the first call for a run has an effect. */
    private void release(ReplyContext reply) {
        activeReplyGuard.release(reply.conversationId(), reply.runId());
    }

    private Message saveAssistant(ReplyContext reply, Completion completion) {
        return transaction.execute(status -> {
            var conversation = conversationService.lock(reply.conversationId());
            var message = messageService.saveAssistant(conversation, reply.model(), completion);
            if (completion.cost() != null) {
                usageService.record(reply.userId(), reply.model(), completion.cost());
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
