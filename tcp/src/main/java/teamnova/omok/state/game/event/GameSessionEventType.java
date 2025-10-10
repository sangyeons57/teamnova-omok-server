package teamnova.omok.state.game.event;

/**
 * Supported event types that game session states can handle.
 */
public enum GameSessionEventType {
    READY,
    MOVE,
    TIMEOUT,
    POST_GAME_DECISION,
    DECISION_TIMEOUT
}
