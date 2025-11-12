package teamnova.omok.glue.game.session.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import org.junit.jupiter.api.Test;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionOutcomeAccess;
import teamnova.omok.glue.game.session.model.PlayerResult;
import teamnova.omok.glue.game.session.model.Stone;
import teamnova.omok.glue.game.session.model.vo.GameSessionId;
import teamnova.omok.support.TestBoardAccess;

final class BoardVictoryResolverTest {
    private final BoardService boardService = new BoardService();

    @Test
    void resolvesWinWhenRuleAddsWildcards() {
        TestBoardAccess board = new TestBoardAccess(10, 10);
        board.putStone(1, 1, Stone.JOKER);
        board.putStone(2, 1, Stone.PLAYER1);
        board.putStone(3, 1, Stone.PLAYER1);
        board.putStone(4, 1, Stone.PLAYER1);
        board.putStone(5, 1, Stone.JOKER);

        StubOutcomeAccess outcomes = new StubOutcomeAccess();
        boolean resolved = BoardVictoryResolver.resolve(
            board,
            List.of("blue", "red"),
            outcomes,
            boardService,
            "test-session"
        );

        assertTrue(resolved);
        assertEquals(PlayerResult.WIN, outcomes.outcomeFor("blue"));
        assertEquals(PlayerResult.LOSS, outcomes.outcomeFor("red"));
    }

    @Test
    void returnsFalseWhenNoSequencePresent() {
        TestBoardAccess board = new TestBoardAccess(10, 10);
        board.putStone(2, 1, Stone.PLAYER1);
        board.putStone(3, 1, Stone.PLAYER1);
        board.putStone(4, 1, Stone.JOKER);

        StubOutcomeAccess outcomes = new StubOutcomeAccess();
        boolean resolved = BoardVictoryResolver.resolve(
            board,
            List.of("blue", "red"),
            outcomes,
            boardService,
            "test-session"
        );

        assertFalse(resolved);
        assertEquals(PlayerResult.LOSS, outcomes.outcomeFor("blue")); // default value
        assertEquals(PlayerResult.LOSS, outcomes.outcomeFor("red"));
    }

    private static final class StubOutcomeAccess implements GameSessionOutcomeAccess {
        private final GameSessionId sessionId = GameSessionId.random();
        private final ReentrantLock lock = new ReentrantLock();
        private final Map<String, PlayerResult> results = new HashMap<>();
        private boolean finished;

        @Override
        public GameSessionId sessionId() {
            return sessionId;
        }

        @Override
        public ReentrantLock lock() {
            return lock;
        }

        @Override
        public void resetOutcomes() {
            results.clear();
            finished = false;
        }

        @Override
        public PlayerResult outcomeFor(String userId) {
            return results.getOrDefault(userId, PlayerResult.LOSS);
        }

        @Override
        public void updateOutcome(String userId, PlayerResult result) {
            results.put(userId, result);
            if (result == PlayerResult.WIN) {
                finished = true;
            }
        }

        @Override
        public boolean isGameFinished() {
            return finished;
        }
    }
}
