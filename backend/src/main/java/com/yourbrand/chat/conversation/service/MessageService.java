package com.yourbrand.chat.conversation.service;

import com.yourbrand.chat.config.ChatProperties;
import com.yourbrand.chat.conversation.entity.Message;
import com.yourbrand.chat.conversation.repository.MessageRepository;
import com.yourbrand.chat.integration.openrouter.ChatCompletionClient.Completion;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;

@Service
public class MessageService {

    private final MessageRepository messages;
    private final ConversationService conversations;
    private final ChatProperties properties;

    MessageService(MessageRepository messages, ConversationService conversations, ChatProperties properties) {
        this.messages = messages;
        this.conversations = conversations;
        this.properties = properties;
    }

    public List<Message> list(UUID userId, UUID conversationId) {
        conversations.getOwned(userId, conversationId);
        return messages.findByConversationIdOrderByCreatedAtAsc(conversationId);
    }

    /** The latest {@code app.chat.history-limit} messages, oldest first: the context sent to the model. */
    public List<Message> recentHistory(UUID conversationId) {
        return messages.findByConversationIdOrderByCreatedAtDesc(conversationId, Limit.of(properties.historyLimit()))
                .reversed();
    }

    public Message saveUser(UUID conversationId, String content) {
        return messages.save(Message.user(conversationId, content));
    }

    public Message saveAssistant(UUID conversationId, String model, Completion completion) {
        return messages.save(Message.assistant(
                conversationId,
                model,
                completion.content(),
                completion.promptTokens(),
                completion.completionTokens(),
                completion.cost()));
    }
}
