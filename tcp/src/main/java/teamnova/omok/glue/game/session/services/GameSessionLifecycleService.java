package teamnova.omok.glue.game.session.services;

import java.util.Objects;

import teamnova.omok.glue.client.session.ClientSessionManager;
import teamnova.omok.glue.game.session.interfaces.GameTurnService;
import teamnova.omok.glue.game.session.interfaces.manager.TurnTimeoutScheduler;
import teamnova.omok.glue.game.session.model.dto.TurnSnapshot;

/**
 * Stateless helpers for disconnect and cleanup operations on game sessions.
 */
public final class GameSessionLifecycleService {

    private GameSessionLifecycleService() {
    }

    public static void leaveByUser(GameSessionDependencies deps,
                                   SessionEventService events,
                                   String userId) {
        Objects.requireNonNull(deps, "deps");
        Objects.requireNonNull(events, "events");
        Objects.requireNonNull(userId, "userId");
        deps.repository().findByUserId(userId).ifPresent(session -> {
            events.cancelAllTimers(session.sessionId());
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
        deps.repository().removeByUserId(userId).ifPresent(removed -> {
            deps.runtime().remove(removed);
            // Explicitly unbind the user's client session from this game session
            try {
                ClientSessionManager.getInstance()
                    .findSession(userId)
                    .ifPresent(h -> h.unbindGameSession(removed.sessionId()));
            } catch (Throwable ignore) {
                // best-effort unbind; do not block cleanup
            }
        });
    }

    public static void handleClientDisconnected(GameSessionDependencies deps,
                                               SessionEventService events,
                                               String userId) {
        Objects.requireNonNull(deps, "deps");
        Objects.requireNonNull(events, "events");
        Objects.requireNonNull(userId, "userId");
        deps.repository().findByUserId(userId).ifPresent(session -> {
            boolean newlyDisconnected;
            boolean shouldSkip = false;
            int expectedTurn = -1;
            session.lock().lock();
            try {
                newlyDisconnected = session.markDisconnected(userId);
                if (session.isGameStarted() && !session.isGameFinished()) {
                    TurnSnapshot snapshot =
                        deps.turnService().snapshot(session);
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
                events.skipTurnForDisconnected(session, userId, expectedTurn);
            }
        });
    }
}
