package teamnova.omok.core.nio;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import teamnova.omok.glue.handler.register.Type;
import teamnova.omok.glue.service.ServiceContainer;

/**
 * Small helper utilities related to ClientSession side-effects and cross-service interactions.
 * Extracted to keep ClientSession focused on I/O and state, reducing its size.
 */
public final class ClientSessions {
    private ClientSessions() {}

    // userId -> active ClientSession
    private static final ConcurrentHashMap<String, ClientSession> USER_SESSIONS = new ConcurrentHashMap<>();

    /**
     * Called right after a session is marked authenticated.
     * Enforces single active session per user: existing different session is notified and closed,
     * then the new session is registered as the active one for the user.
     */
    public static void onAuthenticated(NioReactorServer server, ClientSession newSession) {
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(newSession, "newSession");
        String uid = newSession.authenticatedUserId();
        if (uid == null) {
            return; // not authenticated, nothing to do
        }
        USER_SESSIONS.compute(uid, (key, existing) -> {
            if (existing != null && existing != newSession) {
                notifyAndClose(server, existing);
            }
            return newSession;
        });
    }

    /**
     * Called when a session is closing or timing out to clean up the userId → session index.
     */
    public static void onSessionClosed(ClientSession session) {
        if (session == null) return;
        String uid = session.authenticatedUserId();
        if (uid == null) return;
        USER_SESSIONS.remove(uid, session);
    }

    /**
     * Cancel any outstanding matching ticket associated with the given session's authenticated user.
     * Safe to call even if the session is not authenticated or services are unavailable.
     */
    public static void cancelMatchingIfAuthenticated(ClientSession session) {
        if (session == null) return;
        String uid = session.authenticatedUserId();
        if (uid == null) return;
        try {
            ServiceContainer.getInstance().getMatchingService().cancel(uid);
        } catch (Throwable ignore) {
            // Best-effort cleanup only
        }
    }

    private static void notifyAndClose(NioReactorServer server, ClientSession oldSession) {
        try {
            byte[] msg = "SESSION_REPLACED".getBytes(StandardCharsets.UTF_8);
            oldSession.enqueueResponse(Type.ERROR, 0, msg);
            server.enqueueSelectorTask(oldSession::enableWriteInterest);
        } catch (Throwable ignore) {
            // best-effort notification
        } finally {
            try {
                oldSession.close();
            } catch (Throwable ignore) {
                // ignore
            }
        }
    }
}
