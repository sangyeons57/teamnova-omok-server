package teamnova.omok.glue.client.session.services;

import java.util.Objects;

import teamnova.omok.glue.client.session.interfaces.ClientSessionHandle;
import teamnova.omok.glue.client.session.states.manage.ClientStateType;
import teamnova.omok.glue.game.session.GameSessionManager;
import teamnova.omok.glue.manager.MatchingManager;

/**
 * Encapsulates authentication and lifecycle transitions for managed client sessions.
 */
public final class ClientSessionLifecycleService {
    private final ClientSessionDirectory directory;
    private final ClientSessionMessagePublisher clientPublisher;

    public ClientSessionLifecycleService(ClientSessionDirectory directory,
                                         ClientSessionMessagePublisher clientPublisher) {
        this.directory = Objects.requireNonNull(directory, "directory");
        this.clientPublisher = Objects.requireNonNull(clientPublisher, "clientPublisher");
    }

    public void onAuthenticated(ClientSessionHandle session, String userId, String role, String scope) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(userId, "userId");
        session.markAuthenticated(userId, role, scope);
        ClientSessionHandle previous = directory.bindUser(userId, session);
        if (previous != null && previous != session) {
            clientPublisher.session(previous).notifyReplaced();
            previous.close();
        }
    }

    public void onAuthenticationCleared(ClientSessionHandle session) {
        Objects.requireNonNull(session, "session");
        String userId = session.authenticatedUserId();
        if (userId != null) {
            directory.unbindUser(userId, session);
        }
        session.clearAuthentication();
    }

    public void onSessionClosed(ClientSessionHandle session) {
        Objects.requireNonNull(session, "session");
        directory.remove(session);
    }

    public void registerStateObserver(ClientSessionHandle session) {
        if (session instanceof ManagedClientSession managed) {
            managed.addStateListener(this::handleStateTransition);
        }
    }

    private void handleStateTransition(ClientSessionHandle session,
                                       ClientStateType previous,
                                       ClientStateType current) {
        if (current == ClientStateType.DISCONNECTED) {
            handleDisconnected(session);
        }
    }

    private void handleDisconnected(ClientSessionHandle session) {
        directory.remove(session);
        String userId = session.authenticatedUserId();
        if (userId != null) {
            boolean removed = directory.unbindUser(userId, session);
            if (removed) {
                cancelMatching(userId);
                notifyGameDisconnect(userId);
            }
        }
        session.clearAuthentication();
    }

    private void cancelMatching(String userId) {
        try {
            MatchingManager.getInstance().cancel(userId);
        } catch (Throwable ignore) {
            // best-effort cancellation
        }
    }

    private void notifyGameDisconnect(String userId) {
        try {
            GameSessionManager.getInstance()
                .handleClientDisconnected(userId);
        } catch (Throwable ignore) {
            // swallow to avoid cascading disconnect failures
        }
    }
}
