package com.yourbrand.chat.conversation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.yourbrand.chat.common.exception.BadRequestException;
import com.yourbrand.chat.config.ChatProperties;
import com.yourbrand.chat.conversation.entity.Message;
import com.yourbrand.chat.conversation.service.model.ReplyContext;
import com.yourbrand.chat.conversation.service.model.ReplyListener;
import com.yourbrand.chat.integration.openrouter.ChatCompletionClient;
import com.yourbrand.chat.integration.openrouter.ChatCompletionClient.ChatMessage;
import com.yourbrand.chat.integration.openrouter.ChatCompletionClient.Completion;
import com.yourbrand.chat.integration.openrouter.OpenRouterException;
import com.yourbrand.chat.usage.service.DailyUsage;
import com.yourbrand.chat.usage.service.UsageService;
import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

class ChatReplyServiceTest {

    private static final String MODEL = "test/model";
    private static final BigDecimal COST = new BigDecimal("0.002");

    private final ConversationService conversations = mock(ConversationService.class);
    private final MessageService messages = mock(MessageService.class);
    private final UsageService usage = mock(UsageService.class);
    private final ChatProperties properties = new ChatProperties(
            List.of(new ChatProperties.Model(MODEL, "Test")), BigDecimal.ONE, ZoneId.of("UTC"), 10, 30);
    private final RecordingListener listener = new RecordingListener();
    private final ReplyContext reply =
            new ReplyContext(UUID.randomUUID(), UUID.randomUUID(), MODEL, List.of(new ChatMessage("user", "Hi")));

    @Test
    void streamsDeltasThenSavesTheReplyAndRecordsItsCost() {
        var saved = Message.assistant(reply.conversationId(), MODEL, "Hello", 1, 2, COST);
        when(messages.saveAssistant(eq(reply.conversationId()), eq(MODEL), any())).thenReturn(saved);
        var today = new DailyUsage(COST, BigDecimal.ONE);
        when(usage.today(reply.userId())).thenReturn(today);

        service((model, history, onDelta) -> {
            onDelta.accept("Hel");
            onDelta.accept("lo");
            return new Completion("Hello", 1, 2, COST);
        }).generate(reply, listener);

        assertThat(listener.deltas).containsExactly("Hel", "lo");
        assertThat(listener.completed).isSameAs(saved);
        assertThat(listener.usage).isEqualTo(today);
        verify(usage).record(reply.userId(), MODEL, COST);
    }

    @Test
    void keepsThePartialReplyWithoutChargingWhenTheClientLeaves() {
        listener.leaveAfterDeltas = 1;

        service((model, history, onDelta) -> {
            onDelta.accept("Hel");
            onDelta.accept("lo");
            onDelta.accept(", world");
            return new Completion("Hello, world", 1, 2, COST);
        }).generate(reply, listener);

        var completion = ArgumentCaptor.forClass(Completion.class);
        verify(messages).saveAssistant(eq(reply.conversationId()), eq(MODEL), completion.capture());
        // Everything the model generated is kept, including the delta the client never received.
        assertThat(completion.getValue().content()).isEqualTo("Hello");
        verify(usage, never()).record(any(), anyString(), any());
        assertThat(listener.error).isNull();
    }

    @Test
    void hidesAccountProblemsFromUsers() {
        service((model, history, onDelta) -> {
            throw new OpenRouterException(402, "Insufficient credits");
        }).generate(reply, listener);

        assertThat(listener.error).contains("temporarily unavailable").doesNotContain("credits");
    }

    @Test
    void rejectsMessagesOverTheLengthLimitBeforeSavingAnything() {
        var service = service((model, history, onDelta) -> null);

        assertThatThrownBy(() -> service.acceptUserMessage(reply.userId(), reply.conversationId(), "x".repeat(11), MODEL))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Messages are limited to 10 characters.");
        verify(messages, never()).saveUser(any(), any());
    }

    private ChatReplyService service(ChatCompletionClient client) {
        return new ChatReplyService(
                conversations,
                messages,
                new ModelCatalogService(properties),
                usage,
                client,
                properties,
                new TransactionTemplate(mock(PlatformTransactionManager.class)));
    }

    private static final class RecordingListener implements ReplyListener {
        final List<String> deltas = new ArrayList<>();
        int leaveAfterDeltas = Integer.MAX_VALUE;
        Message completed;
        DailyUsage usage;
        String error;

        @Override
        public void onDelta(String text) {
            if (deltas.size() == leaveAfterDeltas) throw new ClientGoneException();
            deltas.add(text);
        }

        @Override
        public void onComplete(Message message, DailyUsage usage) {
            this.completed = message;
            this.usage = usage;
        }

        @Override
        public void onError(String userFacingMessage) {
            this.error = userFacingMessage;
        }
    }
}
