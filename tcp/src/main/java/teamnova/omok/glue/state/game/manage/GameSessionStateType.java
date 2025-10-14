package teamnova.omok.glue.state.game.manage;

import teamnova.omok.modules.state_machine.models.StateName;

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
    POST_GAME_DECISION_WAITING,
    POST_GAME_DECISION_RESOLVING,
    SESSION_REMATCH_PREPARING,
    SESSION_TERMINATING,
    COMPLETED;

    private StateName name = new StateName(name().toLowerCase());

    public StateName toStateName() {
        return name;
    }
}
