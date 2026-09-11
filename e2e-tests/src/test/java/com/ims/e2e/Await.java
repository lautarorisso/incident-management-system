package com.ims.e2e;

import java.util.function.Supplier;

/**
 * Tiny bounded polling helper (no external dependency on Awaitility).
 * <p>
 * Mirrors the {@code wait_for_notification} loop of {@code scripts/smoke-test.sh}:
 * evaluate the condition, and if it is not yet true, sleep and retry until the
 * deadline elapses.
 */
public final class Await {

    /**
     * Polls {@code condition} until it returns {@code true} or {@code timeoutSeconds}
     * elapse.
     *
     * @param condition         supplier evaluated on every poll (must never throw)
     * @param timeoutSeconds    total polling budget
     * @param intervalSeconds   sleep between polls
     * @return {@code true} if the condition became true within the timeout
     */
    public static boolean await(Supplier<Boolean> condition, long timeoutSeconds, long intervalSeconds) {
        long deadline = System.currentTimeMillis() + timeoutSeconds * 1000;
        while (System.currentTimeMillis() < deadline) {
            if (Boolean.TRUE.equals(condition.get())) {
                return true;
            }
            try {
                Thread.sleep(intervalSeconds * 1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return Boolean.TRUE.equals(condition.get());
    }

    private Await() {
    }
}