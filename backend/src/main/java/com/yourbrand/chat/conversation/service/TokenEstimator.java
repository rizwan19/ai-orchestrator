package com.yourbrand.chat.conversation.service;

import com.yourbrand.chat.integration.openrouter.ChatMessage;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Model-independent token estimate: about three UTF-8 bytes per token, plus allowances for chat framing. Errs on the
 * high side for English and handles multi-byte scripts; it is not an exact tokenizer.
 */
@Component
public class TokenEstimator {

    public long estimateText(String text) {
        long bytes = text.getBytes(StandardCharsets.UTF_8).length;
        return (bytes + 2) / 3;
    }

    public long estimatePromptToken(List<ChatMessage> messages) {
        long total = 3; // Estimated reply framing.
        for (var message : messages) {
            total += 12; // Estimated per-message framing.
            total += estimateText(message.role());
            total += estimateText(message.content());
        }
        return total;
    }
}
