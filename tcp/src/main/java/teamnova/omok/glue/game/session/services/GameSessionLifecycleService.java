package teamnova.omok.glue.game.session.services;

import java.util.Objects;

import teamnova.omok.glue.client.session.ClientSessionManager;
import teamnova.omok.glue.game.session.interfaces.GameTurnService;
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
            try {
                ClientSessionManager.getInstance()
                    .findSession(userId)
                    .ifPresent(h -> h.unbindGameSession(session.sessionId()));
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
            session.lock().lock();
            try {
                newlyDisconnected = session.markDisconnected(userId);
            } finally {
                session.lock().unlock();
            }
            if (newlyDisconnected) {
                deps.messenger().broadcastPlayerDisconnected(session, userId, "DISCONNECTED");
            }
        });
    }
}
