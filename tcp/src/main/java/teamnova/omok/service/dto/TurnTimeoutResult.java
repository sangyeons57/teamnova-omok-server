package teamnova.omok.service.dto;

import teamnova.omok.domain.session.game.GameSession;
import teamnova.omok.domain.session.game.entity.turn.TurnSnapshot;

/**
 * Describes the outcome of a turn-timeout evaluation and the resulting turn snapshots.
 */
public record TurnTimeoutResult(GameSession session,
                                boolean timedOut,
                                TurnSnapshot currentTurn,
                                TurnSnapshot nextTurn,
                                String previousPlayerId) {

    public static TurnTimeoutResult noop(GameSession session, TurnSnapshot current) {
        return new TurnTimeoutResult(session, false, current, current, null);
    }

    public static TurnTimeoutResult timedOut(GameSession session,
                                             TurnSnapshot current,
                                             TurnSnapshot next,
                                             String previousPlayerId) {
        return new TurnTimeoutResult(session, true, current, next, previousPlayerId);
    }
}
