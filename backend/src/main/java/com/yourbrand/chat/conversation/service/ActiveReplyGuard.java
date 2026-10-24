package com.yourbrand.chat.conversation.service;

import com.yourbrand.chat.common.concurrent.CancellationToken;
import com.yourbrand.chat.common.exception.ConflictException;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

/**
 * Allows one reply run per conversation at a time. State is in memory, so this only holds for a single backend
 * instance. A run is released by its worker once it can no longer write, never by a disconnect or the deadline,
 * which only cancel it.
 */
@Component
public class ActiveReplyGuard {

    /** Whole-run deadline, from acceptance to the last write; matches the SSE stream timeout. */
    public static final Duration RUN_DEADLINE = Duration.ofMinutes(5);

    public record ActiveReply(UUID runId, CancellationToken cancellation, CompletableFuture<Void> deadline) {}

    private final ConcurrentHashMap<UUID, ActiveReply> active = new ConcurrentHashMap<>();

    public ActiveReply acquire(UUID conversationId) {
        var cancellation = new CancellationToken();
        // Cancelling this future before it fires stops the delayed cancel from running.
        var deadline = CompletableFuture.runAsync(
                cancellation::cancel,
                CompletableFuture.delayedExecutor(RUN_DEADLINE.toMillis(), TimeUnit.MILLISECONDS));
        var run = new ActiveReply(UUID.randomUUID(), cancellation, deadline);
        if (active.putIfAbsent(conversationId, run) != null) {
            deadline.cancel(false);
            throw new ConflictException("A reply is already being generated for this conversation.");
        }
        return run;
    }

    public ActiveReply require(UUID conversationId, UUID runId) {
        return find(conversationId, runId)
                .orElseThrow(() -> new IllegalStateException("No active reply " + runId + " for " + conversationId));
    }

    /** Idempotent; a run that already finished is ignored. */
    public void cancel(UUID conversationId, UUID runId) {
        find(conversationId, runId).ifPresent(run -> run.cancellation().cancel());
    }

    /** Only the run that holds the guard can release it. */
    public void release(UUID conversationId, UUID runId) {
        var released = new ActiveReply[1];
        active.computeIfPresent(conversationId, (id, run) -> {
            if (!run.runId().equals(runId)) return run;
            released[0] = run;
            return null;
        });
        if (released[0] != null) released[0].deadline().cancel(false);
    }

    private Optional<ActiveReply> find(UUID conversationId, UUID runId) {
        return Optional.ofNullable(active.get(conversationId)).filter(run -> run.runId().equals(runId));
    }
}
