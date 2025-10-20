package teamnova.omok.glue.game.session.model.result;

import teamnova.omok.glue.game.session.interfaces.GameTurnService;
import teamnova.omok.glue.game.session.model.Stone;

/**
 * Represents how a move request impacted the game session.
 */
public record MoveResult(MoveStatus status,
                         Stone placedAs,
                         GameTurnService.TurnSnapshot turnSnapshot,
                         String userId,
                         int x,
                         int y) {

    public static MoveResult success(Stone stone,
                                     GameTurnService.TurnSnapshot nextTurn,
                                     String userId,
                                     int x,
                                     int y) {
        return new MoveResult(MoveStatus.SUCCESS, stone, nextTurn, userId, x, y);
    }

    public static MoveResult invalid(MoveStatus status,
                                     GameTurnService.TurnSnapshot snapshot,
                                     String userId,
                                     int x,
                                     int y) {
        return new MoveResult(status, null, snapshot, userId, x, y);
    }
}
