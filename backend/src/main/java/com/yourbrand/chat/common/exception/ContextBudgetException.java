package com.yourbrand.chat.common.exception;

/** The model input would exceed {@code app.chat.context.max-input-tokens} even after summarizing older history. */
public class ContextBudgetException extends BadRequestException {

    public ContextBudgetException(String message) {
        super(message);
    }
}
