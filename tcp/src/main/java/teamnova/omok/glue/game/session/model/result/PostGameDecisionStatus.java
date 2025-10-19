package teamnova.omok.glue.game.session.model.result;

/**
 * Result codes for post-game decisions.
 */
public enum PostGameDecisionStatus {
    ACCEPTED,
    INVALID_PLAYER,
    ALREADY_DECIDED,
    TIME_WINDOW_CLOSED,
    SESSION_CLOSED,
    SESSION_NOT_FOUND,
    INVALID_PAYLOAD
}
