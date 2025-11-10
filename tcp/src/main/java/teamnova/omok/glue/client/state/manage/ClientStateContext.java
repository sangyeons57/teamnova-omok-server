package teamnova.omok.glue.client.state.manage;

import java.util.Objects;

import teamnova.omok.glue.client.session.interfaces.ClientSessionHandle;
import teamnova.omok.glue.client.session.services.ClientSessionStore;
import teamnova.omok.glue.game.session.GameSessionManager;
import teamnova.omok.glue.game.session.states.GameStateHub;
import teamnova.omok.glue.manager.MatchingManager;
import teamnova.omok.modules.state_machine.interfaces.StateContext;

/**
 * Shared data for client-level state handling.
 */
public final class ClientStateContext implements StateContext {
    private final ClientSessionHandle clientSession;
    private final ClientSessionStore store;
    private GameStateHub gameStateManager;

    public ClientStateContext(ClientSessionHandle clientSession,
                              ClientSessionStore store) {
        this.clientSession = Objects.requireNonNull(clientSession, "clientSession");
        this.store = Objects.requireNonNull(store, "store");
    }

    public ClientSessionHandle clientSession() {
        return clientSession;
    }

    public GameStateHub gameStateManager() {
        return gameStateManager;
    }

    public void attachGame(GameStateHub manager) {
        this.gameStateManager = manager;
    }

    public void clearGame() {
        this.gameStateManager = null;
    }

    public void cleanupDisconnected() {
        store.remove(clientSession);
        String userId = clientSession.authenticatedUserId();
        if (userId != null) {
            boolean removed = store.unbindUser(userId, clientSession);
            if (removed) {
                cancelMatching(userId);
                notifyGameDisconnect(userId);
            }
        }
        clientSession.clearAuthentication();
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
            GameSessionManager.getInstance().handleClientDisconnected(userId);
        } catch (Throwable ignore) {
            // swallow
        }
    }
}
