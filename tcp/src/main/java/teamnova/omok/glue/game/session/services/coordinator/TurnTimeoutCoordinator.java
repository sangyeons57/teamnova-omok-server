package teamnova.omok.glue.game.session.services.coordinator;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import teamnova.omok.glue.game.session.interfaces.GameTurnService;
import teamnova.omok.glue.game.session.interfaces.manager.TurnTimeoutScheduler;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionParticipantsAccess;
import teamnova.omok.glue.game.session.model.GameSession;
import teamnova.omok.glue.game.session.model.dto.TurnSnapshot;
import teamnova.omok.glue.game.session.model.vo.GameSessionId;

/**
 * Coordinates scheduling and cancellation of per-session turn timeouts.
 */
public class TurnTimeoutCoordinator implements TurnTimeoutScheduler {
    private final ScheduledExecutorService scheduler;
    private final ConcurrentMap<GameSessionId, TimeoutRef> tasks = new ConcurrentHashMap<>();

    public TurnTimeoutCoordinator() {
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setName("turn-timeout");
            t.setDaemon(true);
            return t;
        });
    }

    @Override
    public void schedule(GameSessionParticipantsAccess session,
                         TurnSnapshot turnSnapshot,
                         TurnTimeoutConsumer consumer) {
        if (session == null || turnSnapshot == null || consumer == null) {
            return;
        }
        if (turnSnapshot.turnEndAt() <= 0) {
            cancel(session.sessionId());
            return;
        }
        long now = System.currentTimeMillis();
        long delay = Math.max(0L, turnSnapshot.turnEndAt() - now);
        GameSessionId sessionId = session.sessionId();
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

    @Override
    public void cancel(GameSessionId sessionId) {
        if (sessionId == null) return;
        TimeoutRef ref = tasks.remove(sessionId);
        if (ref != null) {
            ref.future().cancel(false);
        }
    }

    @Override
    public boolean validate(GameSessionId sessionId, int expectedTurnNumber) {
        TimeoutRef ref = tasks.get(sessionId);
        return ref != null && ref.turnNumber() == expectedTurnNumber;
    }

    @Override
    public void clearIfMatches(GameSessionId sessionId, int expectedTurnNumber) {
        TimeoutRef ref = tasks.get(sessionId);
        if (ref == null || ref.turnNumber() != expectedTurnNumber) {
            return;
        }
        tasks.remove(sessionId, ref);
    }

    private record TimeoutRef(ScheduledFuture<?> future, int turnNumber) { }
}
