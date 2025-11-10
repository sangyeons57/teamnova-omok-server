package teamnova.omok.glue.client.session.services;

import java.util.Objects;

import teamnova.omok.glue.client.session.interfaces.ClientSessionHandle;

/**
 * Encapsulates authentication and lifecycle transitions for managed client sessions.
 */
public final class ClientSessionLifecycleService {
    private final ClientSessionStore store;
    private final ClientSessionMessagePublisher clientPublisher;

    public ClientSessionLifecycleService(ClientSessionStore store,
                                         ClientSessionMessagePublisher clientPublisher) {
        this.store = Objects.requireNonNull(store, "store");
        this.clientPublisher = Objects.requireNonNull(clientPublisher, "clientPublisher");
    }

    public void onAuthenticated(ClientSessionHandle session, String userId, String role, String scope) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(userId, "userId");
        session.markAuthenticated(userId, role, scope);
        ClientSessionHandle previous = store.bindUser(userId, session);
        if (previous != null && previous != session) {
            clientPublisher.session(previous).notifyReplaced();
            previous.close();
        }
    }

    public void onAuthenticationCleared(ClientSessionHandle session) {
        Objects.requireNonNull(session, "session");
        String userId = session.authenticatedUserId();
        if (userId != null) {
            store.unbindUser(userId, session);
        }
        session.clearAuthentication();
    }

}
