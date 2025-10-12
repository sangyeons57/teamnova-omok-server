package teamnova.omok.service.cordinator;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Coordinates the post-game decision timeout per session.
 */
public class DecisionTimeoutCoordinator {
    private final ScheduledExecutorService scheduler;
    private final ConcurrentMap<UUID, ScheduledFuture<?>> tasks = new ConcurrentHashMap<>();

    public DecisionTimeoutCoordinator() {
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setName("decision-timeout");
            t.setDaemon(true);
            return t;
        });
    }

    public void schedule(UUID sessionId, long deadlineAt, Runnable task) {
        if (sessionId == null || task == null) {
            return;
        }
        long now = System.currentTimeMillis();
        long delay = Math.max(0L, deadlineAt - now);
        tasks.compute(sessionId, (id, previous) -> {
            if (previous != null) {
                previous.cancel(false);
            }
            ScheduledFuture<?> future = scheduler.schedule(() -> {
                try {
                    task.run();
                } finally {
                    tasks.remove(id);
                }
            }, delay, TimeUnit.MILLISECONDS);
            return future;
        });
    }

    public void cancel(UUID sessionId) {
        if (sessionId == null) {
            return;
        }
        ScheduledFuture<?> future = tasks.remove(sessionId);
        if (future != null) {
            future.cancel(false);
        }
    }
}
