package com.yourbrand.chat.openrouter;

/** An error reported by OpenRouter, before or during a stream. The message is safe to show to users. */
public class OpenRouterException extends RuntimeException {

    private final int status;

    public OpenRouterException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int status() {
        return status;
    }
}
