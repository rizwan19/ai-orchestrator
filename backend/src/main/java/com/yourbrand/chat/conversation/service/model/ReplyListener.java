package com.yourbrand.chat.conversation.service.model;

import com.yourbrand.chat.conversation.entity.Message;
import com.yourbrand.chat.usage.service.DailyUsage;

/**
 * Receives a reply as it is generated. Implementations deliver it to the client (e.g. over SSE) and throw
 * {@link ClientGoneException} once nobody is listening any more.
 */
public interface ReplyListener {

    void onDelta(String text);

    void onComplete(Message message, DailyUsage usage);

    /** Must not throw: called when the reply cannot be finished. */
    void onError(String userFacingMessage);

    /** The client closed the stream (navigated away or pressed Stop). */
    final class ClientGoneException extends RuntimeException {}
}
