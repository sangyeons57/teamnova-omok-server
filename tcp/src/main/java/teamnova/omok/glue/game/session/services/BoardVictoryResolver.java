package teamnova.omok.glue.game.session.services;

import java.util.List;
import java.util.Objects;

import teamnova.omok.glue.game.session.interfaces.GameBoardService;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionBoardAccess;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionOutcomeAccess;
import teamnova.omok.glue.game.session.model.PlayerResult;
import teamnova.omok.glue.game.session.model.Stone;

/**
 * Utility that scans the board for five-in-a-row sequences and finalizes outcomes when found.
 */
public final class BoardVictoryResolver {
    private BoardVictoryResolver() { }

    public static boolean resolve(GameSessionBoardAccess board,
                                  List<String> userIds,
                                  GameSessionOutcomeAccess outcomes,
                                  GameBoardService boardService,
                                  String sessionId) {
        Objects.requireNonNull(boardService, "boardService");
        Objects.requireNonNull(board, "board");
        Objects.requireNonNull(outcomes, "outcomes");
        if (outcomes.isGameFinished()) {
            return true;
        }
        if (userIds == null || userIds.isEmpty()) {
            return false;
        }

        for (int index = 0; index < userIds.size(); index++) {
            Stone stone = Stone.fromPlayerOrder(index);
            if (stone == Stone.EMPTY) {
                continue;
            }
            WinningLine line = findWinningLine(board, boardService, stone);
            if (line == null) {
                continue;
            }
            finalizeOutcomes(outcomes, userIds, userIds.get(index), stone, line.x(), line.y(), sessionId);
            return true;
        }
        return false;
    }

    private static WinningLine findWinningLine(GameSessionBoardAccess board,
                                               GameBoardService boardService,
                                               Stone stone) {
        int width = board.width();
        int height = board.height();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (board.stoneAt(x, y) != stone) {
                    continue;
                }
                if (boardService.hasFiveInARow(board, x, y, stone)) {
                    return new WinningLine(x, y);
                }
            }
        }
        return null;
    }

    private static void finalizeOutcomes(GameSessionOutcomeAccess outcomes,
                                         List<String> userIds,
                                         String winnerId,
                                         Stone stone,
                                         int x,
                                         int y,
                                         String sessionId) {
        for (String uid : userIds) {
            PlayerResult result = uid.equals(winnerId) ? PlayerResult.WIN : PlayerResult.LOSS;
            outcomes.updateOutcome(uid, result);
        }
        if (sessionId != null) {
            System.out.printf(
                "[OutcomeService] Game %s finished: winner=%s stone=%s position=(%d,%d)%n",
                sessionId,
                winnerId,
                stone,
                x,
                y
            );
        }
    }

    private record WinningLine(int x, int y) { }
}
