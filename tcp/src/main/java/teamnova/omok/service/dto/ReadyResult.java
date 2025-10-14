package teamnova.omok.service.dto;

import teamnova.omok.domain.session.game.GameSession;
import teamnova.omok.domain.session.game.entity.turn.TurnSnapshot;

/**
 * Aggregates the outcome of a ready-event evaluation for the requesting player.
 */
public record ReadyResult(GameSession session,
                          boolean validUser,
                          boolean stateChanged,
                          boolean allReady,
                          boolean gameStartedNow,
                          TurnSnapshot firstTurn,
                          String userId) {

    public static ReadyResult invalid(GameSession session, String userId) {
        return new ReadyResult(session, false, false, false, false, null, userId);
    }
}
