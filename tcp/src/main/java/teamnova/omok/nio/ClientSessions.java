package teamnova.omok.nio;

import teamnova.omok.service.ServiceContainer;

/**
 * Small helper utilities related to ClientSession side-effects and cross-service interactions.
 * Extracted to keep ClientSession focused on I/O and state, reducing its size.
 */
public final class ClientSessions {
    private ClientSessions() {}

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
}
