package teamnova.omok.service.dto;

import teamnova.omok.domain.session.game.GameSession;
import teamnova.omok.domain.session.game.entity.stone.Stone;
import teamnova.omok.domain.session.game.entity.turn.TurnSnapshot;

/**
 * Represents how a move request impacted the game session.
 */
public record MoveResult(GameSession session,
                         MoveStatus status,
                         Stone placedAs,
                         TurnSnapshot turnSnapshot,
                         String userId,
                         int x,
                         int y) {

    public static MoveResult success(GameSession session,
                                     Stone stone,
                                     TurnSnapshot nextTurn,
                                     String userId,
                                     int x,
                                     int y) {
        return new MoveResult(session, MoveStatus.SUCCESS, stone, nextTurn, userId, x, y);
    }

    public static MoveResult invalid(GameSession session,
                                     MoveStatus status,
                                     TurnSnapshot snapshot,
                                     String userId,
                                     int x,
                                     int y) {
        return new MoveResult(session, status, null, snapshot, userId, x, y);
    }
}
