package teamnova.omok.glue.client.session.states.manage;

import java.util.Objects;

import teamnova.omok.glue.client.session.interfaces.ClientSessionHandle;
import teamnova.omok.glue.game.session.states.GameStateHub;
import teamnova.omok.modules.state_machine.interfaces.StateContext;

/**
 * Shared data for client-level state handling.
 */
public final class ClientStateContext implements StateContext {
    private final ClientSessionHandle clientSession;
    private GameStateHub gameStateManager;

    public ClientStateContext(ClientSessionHandle clientSession) {
        this.clientSession = Objects.requireNonNull(clientSession, "clientSession");
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
}
