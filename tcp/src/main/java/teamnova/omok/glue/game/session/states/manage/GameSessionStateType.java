package teamnova.omok.glue.game.session.states.manage;

import teamnova.omok.modules.state_machine.models.StateName;

/**
 * Logical phases of a game session lifecycle.
 */
public enum GameSessionStateType {
    LOBBY,
    TURN_START,
    TURN_PERSONAL_START,
    TURN_WAITING,
    MOVE_VALIDATING,
    MOVE_APPLYING,
    OUTCOME_EVALUATING,
    TURN_PERSONAL_END,
    TURN_END,
    POST_GAME_DECISION_WAITING,
    POST_GAME_DECISION_RESOLVING,
    SESSION_REMATCH_PREPARING,
    SESSION_TERMINATING,
    COMPLETED;

    private final StateName name;

    GameSessionStateType() {
        this.name = new StateName(name().toLowerCase());
    }

    public StateName toStateName() {
        return name;
    }

    public static GameSessionStateType stateNameLookup(StateName name) {
        return switch (name.name()) {
            case "lobby", "LOBBY" -> LOBBY;
            case "turn_starting", "TURN_STARTING" -> TURN_START;
            case "turn_personal_starting", "TURN_PERSONAL_STARTING" -> TURN_PERSONAL_START;
            case "turn_waiting", "TURN_WAITING" -> TURN_WAITING;
            case "move_validating", "MOVE_VALIDATING" -> MOVE_VALIDATING;
            case "move_applying", "MOVE_APPLYING" -> MOVE_APPLYING;
            case "outcome_evaluating", "OUTCOME_EVALUATING" -> OUTCOME_EVALUATING;
            case "turn_personal_completed", "TURN_PERSONAL_COMPLETED", "turn_finalizing", "TURN_FINALIZING" -> TURN_PERSONAL_END;
            case "turn_round_completed", "TURN_ROUND_COMPLETED" -> TURN_END;
            case "post_game_decision_waiting", "POST_GAME_DECISION_WAITING" -> POST_GAME_DECISION_WAITING;
            case "post_game_decision_resolving", "POST_GAME_DECISION_RESOLVING" -> POST_GAME_DECISION_RESOLVING;
            case "session_rematch_preparing", "SESSION_REMATCH_PREPARING" -> SESSION_REMATCH_PREPARING;
            case "session_terminating", "SESSION_TERMINATING" -> SESSION_TERMINATING;
            case "completed", "COMPLETED" -> COMPLETED;
            default -> throw new IllegalArgumentException("Unknown state name: " + name);
        };
    }
}
