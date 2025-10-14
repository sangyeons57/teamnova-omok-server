package teamnova.omok.glue.service.dto;

import teamnova.omok.glue.service.TurnService;
import teamnova.omok.glue.store.GameSession;
import teamnova.omok.glue.store.Stone;

/**
 * Represents how a move request impacted the game session.
 */
public record MoveResult(GameSession session,
                         MoveStatus status,
                         Stone placedAs,
                         TurnService.TurnSnapshot turnSnapshot,
                         String userId,
                         int x,
                         int y) {

    public static MoveResult success(GameSession session,
                                     Stone stone,
                                     TurnService.TurnSnapshot nextTurn,
                                     String userId,
                                     int x,
                                     int y) {
        return new MoveResult(session, MoveStatus.SUCCESS, stone, nextTurn, userId, x, y);
    }

    public static MoveResult invalid(GameSession session,
                                     MoveStatus status,
                                     TurnService.TurnSnapshot snapshot,
                                     String userId,
                                     int x,
                                     int y) {
        return new MoveResult(session, status, null, snapshot, userId, x, y);
    }
}
