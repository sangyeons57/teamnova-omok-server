package teamnova.omok.glue.game.session.services;

import java.util.Objects;

import teamnova.omok.glue.game.session.interfaces.GameTurnService;
import teamnova.omok.glue.game.session.interfaces.TurnTimeoutScheduler;

/**
 * Stateless helpers for disconnect and cleanup operations on game sessions.
 */
public final class GameSessionLifecycleService {

    private GameSessionLifecycleService() {
    }

    public static void leaveByUser(GameSessionDependencies deps, String userId) {
        Objects.requireNonNull(deps, "deps");
        Objects.requireNonNull(userId, "userId");
        deps.repository().findByUserId(userId).ifPresent(session -> {
            SessionEventService.cancelAllTimers(deps, session.sessionId());
            boolean newlyDisconnected;
            session.lock().lock();
            try {
                newlyDisconnected = session.markDisconnected(userId);
            } finally {
                session.lock().unlock();
            }
            if (newlyDisconnected) {
                deps.messenger().broadcastPlayerDisconnected(session, userId, "LEFT");
            }
        });
        deps.repository().removeByUserId(userId).ifPresent(deps.runtime()::remove);
    }

    public static void handleClientDisconnected(GameSessionDependencies deps,
                                                TurnTimeoutScheduler.TurnTimeoutConsumer timeoutConsumer,
                                                String userId) {
        Objects.requireNonNull(deps, "deps");
        Objects.requireNonNull(timeoutConsumer, "timeoutConsumer");
        Objects.requireNonNull(userId, "userId");
        deps.repository().findByUserId(userId).ifPresent(session -> {
            boolean newlyDisconnected;
            boolean shouldSkip = false;
            int expectedTurn = -1;
            session.lock().lock();
            try {
                newlyDisconnected = session.markDisconnected(userId);
                if (session.isGameStarted() && !session.isGameFinished()) {
                    GameTurnService.TurnSnapshot snapshot =
                        deps.turnService().snapshot(session.getTurnStore());
                    if (snapshot != null && userId.equals(snapshot.currentPlayerId())) {
                        shouldSkip = true;
                        expectedTurn = snapshot.turnNumber();
                    }
                }
            } finally {
                session.lock().unlock();
            }
            if (newlyDisconnected) {
                deps.messenger().broadcastPlayerDisconnected(session, userId, "DISCONNECTED");
            }
            if (shouldSkip && expectedTurn > 0) {
                SessionEventService.skipTurnForDisconnected(deps, timeoutConsumer, session, userId, expectedTurn);
            }
        });
    }
}
