package teamnova.omok.glue.state.client.event;

import java.util.Objects;

import teamnova.omok.glue.state.game.GameStateHub;

public final class EnterGameClientEvent implements ClientEvent {
    private final GameStateHub gameStateManager;

    public EnterGameClientEvent(GameStateHub gameStateManager) {
        this.gameStateManager = Objects.requireNonNull(gameStateManager, "gameStateManager");
    }

    public GameStateHub gameStateManager() {
        return gameStateManager;
    }

    @Override
    public ClientEventType type() {
        return ClientEventType.ENTER_GAME;
    }
}
