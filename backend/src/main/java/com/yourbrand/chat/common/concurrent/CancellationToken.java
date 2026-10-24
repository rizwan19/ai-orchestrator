package com.yourbrand.chat.common.concurrent;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/** Cooperative, idempotent cancellation shared between a reply worker and whoever may stop it (disconnect, deadline). */
public final class CancellationToken {

    private final AtomicBoolean cancelled = new AtomicBoolean();
    private final Set<Registration> registrations = ConcurrentHashMap.newKeySet();

    public boolean isCancelled() {
        return cancelled.get();
    }

    public void cancel() {
        if (cancelled.compareAndSet(false, true)) {
            registrations.forEach(Registration::fire);
        }
    }

    public void throwIfCancelled() {
        if (isCancelled()) throw new OperationCancelledException();
    }

    /**
     * Runs {@code action} once on cancellation, or immediately when already cancelled. Once the returned registration
     * is closed the action will not run, and is not running.
     */
    public Registration onCancel(Runnable action) {
        var registration = new Registration(action);
        registrations.add(registration);
        if (isCancelled()) registration.fire();
        return registration;
    }

    public final class Registration implements AutoCloseable {

        private final Runnable action;
        private boolean done;

        private Registration(Runnable action) {
            this.action = action;
        }

        private synchronized void fire() {
            if (done) return;
            done = true;
            action.run();
        }

        @Override
        public synchronized void close() {
            done = true;
            registrations.remove(this);
        }
    }
}
