package com.yourbrand.chat.conversation.web;

import com.yourbrand.chat.conversation.dto.response.ReplyDeltaEvent;
import com.yourbrand.chat.conversation.dto.response.ReplyDoneEvent;
import com.yourbrand.chat.conversation.dto.response.ReplyErrorEvent;
import com.yourbrand.chat.conversation.entity.Message;
import com.yourbrand.chat.conversation.mapper.MessageMapper;
import com.yourbrand.chat.conversation.service.model.ReplyListener;
import com.yourbrand.chat.usage.mapper.UsageMapper;
import com.yourbrand.chat.usage.service.DailyUsage;
import java.io.IOException;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/** Streams a reply as SSE events: {@code delta} {text}, then {@code done} {message, usage} or {@code error} {message}. */
public class SseReplyListener implements ReplyListener {

    private final SseEmitter emitter;
    private final MessageMapper messageMapper;
    private final UsageMapper usageMapper;

    public SseReplyListener(SseEmitter emitter, MessageMapper messageMapper, UsageMapper usageMapper) {
        this.emitter = emitter;
        this.messageMapper = messageMapper;
        this.usageMapper = usageMapper;
    }

    @Override
    public void onDelta(String text) {
        send("delta", new ReplyDeltaEvent(text));
    }

    @Override
    public void onComplete(Message message, DailyUsage usage) {
        send("done", new ReplyDoneEvent(messageMapper.toResponse(message), usageMapper.toResponse(usage)));
        emitter.complete();
    }

    @Override
    public void onError(String userFacingMessage) {
        try {
            send("error", new ReplyErrorEvent(userFacingMessage));
            emitter.complete();
        } catch (ClientGoneException ignored) {
            // Nobody is listening any more.
        }
    }

    private void send(String event, Object data) {
        try {
            emitter.send(SseEmitter.event().name(event).data(data, MediaType.APPLICATION_JSON));
        } catch (IOException | IllegalStateException e) {
            throw new ClientGoneException();
        }
    }
}
