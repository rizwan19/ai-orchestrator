package com.yourbrand.chat.integration.openrouter;

/** A non-streamed completion; {@code finishReason} is e.g. {@code stop}, {@code length} or {@code content_filter}. */
public record CompletionResult(Completion completion, String finishReason) {}
