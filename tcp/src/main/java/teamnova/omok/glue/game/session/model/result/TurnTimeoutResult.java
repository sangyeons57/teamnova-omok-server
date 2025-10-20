package teamnova.omok.glue.game.session.model.result;

import teamnova.omok.glue.game.session.interfaces.GameTurnService;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionAccess;
import teamnova.omok.glue.game.session.model.GameSession;

/**
 * Describes the outcome of a turn-timeout evaluation and the resulting turn snapshots.
 */
public record TurnTimeoutResult(GameSessionAccess session,
                                boolean timedOut,
                                GameTurnService.TurnSnapshot currentTurn,
                                GameTurnService.TurnSnapshot nextTurn,
                                String previousPlayerId) {

    public static TurnTimeoutResult noop(GameSessionAccess session, GameTurnService.TurnSnapshot current) {
        return new TurnTimeoutResult(session, false, current, current, null);
    }

    public static TurnTimeoutResult timedOut(GameSessionAccess session,
                                             GameTurnService.TurnSnapshot current,
                                             GameTurnService.TurnSnapshot next,
                                             String previousPlayerId) {
        return new TurnTimeoutResult(session, true, current, next, previousPlayerId);
    }
}
