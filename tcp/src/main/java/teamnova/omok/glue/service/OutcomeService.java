package teamnova.omok.glue.service;

import java.util.List;
import java.util.Objects;

import teamnova.omok.glue.game.PlayerResult;
import teamnova.omok.glue.store.GameSession;
import teamnova.omok.glue.store.Stone;

/**
 * Handles simple outcome updates such as five-in-a-row victories.
 */
public class OutcomeService {
    private final BoardService boardService;

    public OutcomeService(BoardService boardService) {
        this.boardService = Objects.requireNonNull(boardService, "boardService");
    }

    /**
     * Processes a newly placed stone and updates the game outcome if a five-in-a-row is detected.
     *
     * @return true if the game finished as a result of this move.
     */
    public boolean handleStonePlaced(GameSession session, String userId, int x, int y, Stone stone) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(stone, "stone");

        if (session.isGameFinished()) {
            return true;
        }

        if (!boardService.hasFiveInARow(session.getBoardStore(), x, y, stone)) {
            return false;
        }

        for (String disconnectedId : session.disconnectedUsersView()) {
            if (!disconnectedId.equals(userId)) {
                session.updateOutcome(disconnectedId, PlayerResult.LOSS);
            }
        }
        session.updateOutcome(userId, PlayerResult.WIN);
        List<String> userIds = session.getUserIds();
        for (String uid : userIds) {
            if (!uid.equals(userId)) {
                session.updateOutcome(uid, PlayerResult.LOSS);
            }
        }
        System.out.printf(
            "[OutcomeService] Game %s finished: winner=%s stone=%s position=(%d,%d)%n",
            session.getId(),
            userId,
            stone,
            x,
            y
        );
        return true;
    }
}
