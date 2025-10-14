package teamnova.omok.glue.service.cordinator;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import teamnova.omok.glue.service.TurnService;
import teamnova.omok.glue.store.GameSession;

/**
 * Coordinates scheduling and cancellation of per-session turn timeouts.
 */
public class TurnTimeoutCoordinator {
    private final ScheduledExecutorService scheduler;
    private final ConcurrentMap<UUID, TimeoutRef> tasks = new ConcurrentHashMap<>();

    public TurnTimeoutCoordinator() {
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setName("turn-timeout");
            t.setDaemon(true);
            return t;
        });
    }

    public void schedule(GameSession session,
                         TurnService.TurnSnapshot turnSnapshot,
                         TurnTimeoutConsumer consumer) {
        if (session == null || turnSnapshot == null || consumer == null) {
            return;
        }
        if (turnSnapshot.turnEndAt() <= 0) {
            cancel(session.getId());
            return;
        }
        long now = System.currentTimeMillis();
        long delay = Math.max(0L, turnSnapshot.turnEndAt() - now);
        UUID sessionId = session.getId();
        tasks.compute(sessionId, (id, previous) -> {
            if (previous != null) {
                previous.future().cancel(false);
            }
            ScheduledFuture<?> future = scheduler.schedule(
                () -> consumer.onTimeout(sessionId, turnSnapshot.turnNumber()),
                delay,
                TimeUnit.MILLISECONDS
            );
            return new TimeoutRef(future, turnSnapshot.turnNumber());
        });
    }

    public void cancel(UUID sessionId) {
        if (sessionId == null) return;
        TimeoutRef ref = tasks.remove(sessionId);
        if (ref != null) {
            ref.future().cancel(false);
        }
    }

    public boolean validate(UUID sessionId, int expectedTurnNumber) {
        TimeoutRef ref = tasks.get(sessionId);
        return ref != null && ref.turnNumber() == expectedTurnNumber;
    }

    public void clearIfMatches(UUID sessionId, int expectedTurnNumber) {
        TimeoutRef ref = tasks.get(sessionId);
        if (ref == null || ref.turnNumber() != expectedTurnNumber) {
            return;
        }
        tasks.remove(sessionId, ref);
    }

    public interface TurnTimeoutConsumer {
        void onTimeout(UUID sessionId, int expectedTurnNumber);
    }

    private record TimeoutRef(ScheduledFuture<?> future, int turnNumber) { }
}
