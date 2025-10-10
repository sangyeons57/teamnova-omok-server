package teamnova.omok.state.game.manage;

/**
 * Logical phases of a game session lifecycle.
 */
public enum GameSessionStateType {
    LOBBY,
    TURN_WAITING,
    MOVE_VALIDATING,
    MOVE_APPLYING,
    OUTCOME_EVALUATING,
    TURN_FINALIZING,
    COMPLETED
}
