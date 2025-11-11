package teamnova.omok.glue.client.session.services;

import java.util.Objects;

import teamnova.omok.core.nio.NioClientConnection;
import teamnova.omok.core.nio.NioReactorServer;
import teamnova.omok.glue.client.session.interfaces.ClientSessionHandle;

/**
 * Encapsulates authentication and lifecycle transitions for managed client sessions.
 */
public final class ClientSessionLifecycleService {

    public ClientSessionLifecycleService() {
        //stateless
    }

    public void onAuthenticated(
            ClientSessionStore store,
            ClientSessionMessagePublisher publisher,
            ClientSessionHandle session,
            String userId,
            String role,
            String scope
    ) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(userId, "userId");
        session.markAuthenticated(userId, role, scope);
        ClientSessionHandle previous = store.bindUser(session, userId);

        // 리커넥인 경우 실행 되는 기능
        if (previous != null && previous != session) {
            session.attachClientSession(previous);
            publisher.session(previous).notifyReplaced();
            previous.shutdownTransport();
        }
    }

    public void onAuthenticationCleared(ClientSessionStore store, ClientSessionHandle session) {
        Objects.requireNonNull(session, "session");
        String userId = session.authenticatedUserId();
        if (userId != null) {
            store.unbindUser(userId, session);
        }
        session.clearAuthentication();
    }

}
