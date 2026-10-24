package com.yourbrand.chat.common.concurrent;

/** Work stopped because its {@link CancellationToken} was cancelled. */
public class OperationCancelledException extends RuntimeException {

    public OperationCancelledException() {
        super("The operation was cancelled.");
    }
}
