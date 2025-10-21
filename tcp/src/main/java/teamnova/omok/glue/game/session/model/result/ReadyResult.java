package teamnova.omok.glue.game.session.model.result;

import teamnova.omok.glue.game.session.model.dto.TurnSnapshot;

/**
 * Aggregates the outcome of a ready-event evaluation for the requesting player.
 */
public record ReadyResult(boolean validUser,
                          boolean stateChanged,
                          boolean allReady,
                          boolean gameStartedNow,
                          TurnSnapshot firstTurn,
                          String userId) {

    public static ReadyResult invalid(String userId) {
        return new ReadyResult(false, false, false, false, null, userId);
    }
}
