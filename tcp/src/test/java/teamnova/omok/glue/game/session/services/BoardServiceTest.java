package teamnova.omok.glue.game.session.services;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import teamnova.omok.glue.game.session.model.Stone;
import teamnova.omok.support.TestBoardAccess;

final class BoardServiceTest {
    private BoardService service;

    @BeforeEach
    void setUp() {
        service = new BoardService();
    }

    @Test
    void hasFiveInARowCountsWildcardsOnEdges() {
        TestBoardAccess board = new TestBoardAccess(10, 10);
        board.putStone(1, 1, Stone.JOKER);
        board.putStone(2, 1, Stone.PLAYER1);
        board.putStone(3, 1, Stone.PLAYER1);
        board.putStone(4, 1, Stone.PLAYER1);
        board.putStone(5, 1, Stone.JOKER);

        assertTrue(service.hasFiveInARow(board, 3, 1, Stone.PLAYER1));
    }

    @Test
    void hasFiveInARowRequiresFiveContiguousStones() {
        TestBoardAccess board = new TestBoardAccess(10, 10);
        board.putStone(1, 1, Stone.PLAYER1);
        board.putStone(2, 1, Stone.PLAYER1);
        board.putStone(3, 1, Stone.PLAYER1);
        board.putStone(4, 1, Stone.JOKER);   // Wildcard but only makes 4 contiguous stones

        assertFalse(service.hasFiveInARow(board, 2, 1, Stone.PLAYER1));
    }

}
