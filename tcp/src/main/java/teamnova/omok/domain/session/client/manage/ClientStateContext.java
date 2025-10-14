package teamnova.omok.domain.session.client.manage;

import java.util.Objects;

import teamnova.omok.nio.ClientSession;

/**
 * Shared data for client-level state handling.
 */
public final class ClientStateContext {
    private final ClientSession clientSession;
    private GameSessionStateManager gameStateManager;

    public ClientStateContext(ClientSession clientSession) {
        this.clientSession = Objects.requireNonNull(clientSession, "clientSession");
    }

    public ClientSession clientSession() {
        return clientSession;
    }

    public GameSessionStateManager gameStateManager() {
        return gameStateManager;
    }

    public void attachGame(GameSessionStateManager manager) {
        this.gameStateManager = manager;
    }

    public void clearGame() {
        this.gameStateManager = null;
    }
}
