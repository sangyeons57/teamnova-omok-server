package teamnova.omok.glue.service.dto;

import teamnova.omok.glue.service.TurnService;
import teamnova.omok.glue.store.GameSession;

/**
 * Describes the outcome of a turn-timeout evaluation and the resulting turn snapshots.
 */
public record TurnTimeoutResult(GameSession session,
                                boolean timedOut,
                                TurnService.TurnSnapshot currentTurn,
                                TurnService.TurnSnapshot nextTurn,
                                String previousPlayerId) {

    public static TurnTimeoutResult noop(GameSession session, TurnService.TurnSnapshot current) {
        return new TurnTimeoutResult(session, false, current, current, null);
    }

    public static TurnTimeoutResult timedOut(GameSession session,
                                             TurnService.TurnSnapshot current,
                                             TurnService.TurnSnapshot next,
                                             String previousPlayerId) {
        return new TurnTimeoutResult(session, true, current, next, previousPlayerId);
    }
}
