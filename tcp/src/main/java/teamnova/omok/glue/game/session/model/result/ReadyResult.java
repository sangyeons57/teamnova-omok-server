package teamnova.omok.glue.game.session.model.result;

import teamnova.omok.glue.game.session.interfaces.GameTurnService;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionAccess;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionParticipantsAccess;
import teamnova.omok.glue.game.session.model.GameSession;

/**
 * Aggregates the outcome of a ready-event evaluation for the requesting player.
 */
public record ReadyResult(GameSessionAccess session,
                          boolean validUser,
                          boolean stateChanged,
                          boolean allReady,
                          boolean gameStartedNow,
                          GameTurnService.TurnSnapshot firstTurn,
                          String userId) {

    public static ReadyResult invalid(GameSessionAccess session, String userId) {
        return new ReadyResult(session, false, false, false, false, null, userId);
    }
}
