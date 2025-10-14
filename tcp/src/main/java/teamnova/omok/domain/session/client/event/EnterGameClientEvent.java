package teamnova.omok.domain.session.client.event;

import java.util.Objects;

public final class EnterGameClientEvent implements ClientEvent {
    private final GameSessionStateManager gameStateManager;

    public EnterGameClientEvent(GameSessionStateManager gameStateManager) {
        this.gameStateManager = Objects.requireNonNull(gameStateManager, "gameStateManager");
    }

    public GameSessionStateManager gameStateManager() {
        return gameStateManager;
    }

    @Override
    public ClientEventType type() {
        return ClientEventType.ENTER_GAME;
    }
}
