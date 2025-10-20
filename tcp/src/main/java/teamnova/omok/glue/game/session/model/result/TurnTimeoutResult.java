package teamnova.omok.glue.game.session.model.result;

import teamnova.omok.glue.game.session.interfaces.GameTurnService;
/**
 * Describes the outcome of a turn-timeout evaluation and the resulting turn snapshots.
 */
public record TurnTimeoutResult(boolean timedOut,
                                GameTurnService.TurnSnapshot currentTurn,
                                GameTurnService.TurnSnapshot nextTurn,
                                String previousPlayerId) {

    public static TurnTimeoutResult noop(GameTurnService.TurnSnapshot current) {
        return new TurnTimeoutResult(false, current, current, null);
    }

    public static TurnTimeoutResult timedOut(GameTurnService.TurnSnapshot current,
                                             GameTurnService.TurnSnapshot next,
                                             String previousPlayerId) {
        return new TurnTimeoutResult(true, current, next, previousPlayerId);
    }
}
