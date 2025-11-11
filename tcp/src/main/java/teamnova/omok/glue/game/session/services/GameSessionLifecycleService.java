package teamnova.omok.glue.game.session.services;

import java.util.List;
import java.util.Objects;

import teamnova.omok.glue.client.session.ClientSessionManager;
import teamnova.omok.glue.game.session.model.GameSession;

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
            cleanupIfSessionFullyDisconnected(deps, session);
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
            cleanupIfSessionFullyDisconnected(deps, session);
        });
    }

    public static boolean handleClientReconnected(GameSessionDependencies deps,
                                                  SessionEventService events,
                                                  String userId) {
        Objects.requireNonNull(deps, "deps");
        Objects.requireNonNull(events, "events");
        Objects.requireNonNull(userId, "userId");
        System.out.println("[RECONNECT][Lifecycle] attempt user=" + userId);
        return deps.repository().findByUserId(userId)
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
                System.out.println("[RECONNECT][Lifecycle] rebound user=" + userId
                    + " session=" + session.sessionId());
                deps.messenger().broadcastBoardSnapshot(session);
                System.out.println("[RECONNECT][Lifecycle] broadcast board snapshot for user=" + userId);
                return true;
            })
            .orElse(false);
    }

    private static void cleanupIfSessionFullyDisconnected(GameSessionDependencies deps,
                                                          GameSession session) {
        Objects.requireNonNull(session, "session");
        boolean shouldCleanup;
        session.lock().lock();
        try {
            shouldCleanup = !session.getUserIds().isEmpty()
                && session.disconnectedUsersView().containsAll(session.getUserIds());
        } finally {
            session.lock().unlock();
        }
        if (!shouldCleanup) {
            return;
        }
        finalizeSession(deps, session);
    }

    private static void finalizeSession(GameSessionDependencies deps,
                                        GameSession session) {
        List<String> userIds = List.copyOf(session.getUserIds());
        var sessionId = session.sessionId();
        deps.turnTimeoutScheduler().cancel(sessionId);
        deps.decisionTimeoutScheduler().cancel(sessionId);
        deps.runtime().remove(sessionId);
        boolean removed = deps.repository().removeById(sessionId).isPresent();
        if (!removed) {
            return;
        }
        try {
            deps.messenger().broadcastSessionTerminated(session, userIds);
        } catch (Throwable ignore) {
            // ensure cleanup continues even if broadcasting fails
        }
        for (String userId : userIds) {
            try {
                ClientSessionManager.getInstance()
                    .findSession(userId)
                    .ifPresent(handle -> handle.unbindGameSession(sessionId));
            } catch (Throwable ignore) {
                // continue best-effort unbind
            }
        }
    }
}
