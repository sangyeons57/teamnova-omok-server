package teamnova.omok.glue.game.session.model.result;

import teamnova.omok.glue.game.session.interfaces.GameTurnService;
import teamnova.omok.glue.game.session.model.GameSession;
import teamnova.omok.glue.game.session.model.Stone;

/**
 * Represents how a move request impacted the game session.
 */
public record MoveResult(GameSession session,
                         MoveStatus status,
                         Stone placedAs,
                         GameTurnService.TurnSnapshot turnSnapshot,
                         String userId,
                         int x,
                         int y) {

    public static MoveResult success(GameSession session,
                                     Stone stone,
                                     GameTurnService.TurnSnapshot nextTurn,
                                     String userId,
                                     int x,
                                     int y) {
        return new MoveResult(session, MoveStatus.SUCCESS, stone, nextTurn, userId, x, y);
    }

    public static MoveResult invalid(GameSession session,
                                     MoveStatus status,
                                     GameTurnService.TurnSnapshot snapshot,
                                     String userId,
                                     int x,
                                     int y) {
        return new MoveResult(session, status, null, snapshot, userId, x, y);
    }
}
