package teamnova.omok.glue.game.session.services;

import java.util.Objects;

import teamnova.omok.glue.client.session.ClientSessionManager;
import teamnova.omok.glue.game.session.interfaces.GameTurnService;
import teamnova.omok.glue.game.session.model.dto.TurnSnapshot;
import teamnova.omok.glue.game.session.model.vo.GameSessionId;

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

    public static boolean handleClientReconnected(GameSessionDependencies deps,
                                                  SessionEventService events,
                                                  String userId,
                                                  GameSessionId expectedSessionId) {
        Objects.requireNonNull(deps, "deps");
        Objects.requireNonNull(events, "events");
        Objects.requireNonNull(userId, "userId");
        return deps.repository().findByUserId(userId)
            .filter(session -> expectedSessionId == null || session.sessionId().equals(expectedSessionId))
            .map(session -> {
                session.lock().lock();
                try {
                    session.clearDisconnected(userId);
                } finally {
                    session.lock().unlock();
                }
                try {
                    ClientSessionManager.getInstance()
                        .findSession(userId)
                        .ifPresent(handle -> handle.bindGameSession(session.sessionId()));
                } catch (Throwable ignore) {
                    // best-effort bind
                }
                deps.messenger().broadcastBoardSnapshot(session);
                return true;
            })
            .orElse(false);
    }
}
