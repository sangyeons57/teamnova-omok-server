package teamnova.omok.glue.state.client.manage;

import java.util.Objects;

import teamnova.omok.core.nio.ClientSession;
import teamnova.omok.glue.state.game.GameStateHub;

/**
 * Shared data for client-level state handling.
 */
public final class ClientStateContext {
    private final ClientSession clientSession;
    private GameStateHub gameStateManager;

    public ClientStateContext(ClientSession clientSession) {
        this.clientSession = Objects.requireNonNull(clientSession, "clientSession");
    }

    public ClientSession clientSession() {
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
