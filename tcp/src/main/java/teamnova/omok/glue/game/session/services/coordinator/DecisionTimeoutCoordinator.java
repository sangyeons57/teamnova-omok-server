package teamnova.omok.glue.game.session.services.coordinator;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import teamnova.omok.glue.game.session.interfaces.DecisionTimeoutScheduler;
import teamnova.omok.glue.game.session.model.vo.GameSessionId;

/**
 * Coordinates the post-game decision timeout per session.
 */
public class DecisionTimeoutCoordinator implements DecisionTimeoutScheduler {
    private final ScheduledExecutorService scheduler;
    private final ConcurrentMap<GameSessionId, ScheduledFuture<?>> tasks = new ConcurrentHashMap<>();

    public DecisionTimeoutCoordinator() {
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setName("decision-timeout");
            t.setDaemon(true);
            return t;
        });
    }

    @Override
    public void schedule(GameSessionId sessionId, long deadlineAt, Runnable task) {
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

    @Override
    public void cancel(GameSessionId sessionId) {
        if (sessionId == null) {
            return;
        }
        ScheduledFuture<?> future = tasks.remove(sessionId);
        if (future != null) {
            future.cancel(false);
        }
    }
}
