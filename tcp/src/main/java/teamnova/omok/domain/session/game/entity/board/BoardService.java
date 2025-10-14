package teamnova.omok.domain.session.game.entity.board;

import teamnova.omok.domain.session.game.entity.stone.Stone;

/**
 * Holds the raw board state for a single game session using a compact byte array.
 */
public class BoardService {
    public static final int DEFAULT_WIDTH = 10;
    public static final int DEFAULT_HEIGHT = 10;

    private final Board board;

    public BoardService (Board board) {
        this.board = board;
    }
    public void reset() {
        board.clear();
    }

    public boolean isWithinBounds(int x, int y) {
        return x >= 0 && x < board.width() && y >= 0 && y < board.height();
    }

    public boolean isEmpty(int x, int y) {
        return stoneAt(x, y) == Stone.EMPTY;
    }

    public Stone stoneAt(int x, int y) {
        return Stone.fromByte(board.get(linearIndex(x, y)));
    }

    public void setStone(int x, int y, Stone stone) {
        board.set(linearIndex(x, y), stone.code());
    }

    public byte[] snapshot() {
        return board.snapshot();
    }

    public boolean hasFiveInARow(int x, int y, Stone stone) {
        if (stone == null || stone == Stone.EMPTY || stone.isBlocking()) {
            return false;
        }
        if (!stone.isPlayerStone()) {
            return false;
        }
        int[][] directions = {
                {1, 0}, // horizontal
                {0, 1}, // vertical
                {1, 1}, // diagonal down-right
                {1, -1} // diagonal up-right
        };
        for (int[] dir : directions) {
            int count = 1;
            count += countDirection(x, y, dir[0], dir[1], stone);
            count += countDirection(x, y, -dir[0], -dir[1], stone);
            if (count >= 5) {
                return true;
            }
        }
        return false;
    }

    private int linearIndex(int x, int y) {
        return y * board.width() + x;
    }

    private int countDirection(int startX, int startY, int dx, int dy, Stone playerStone) {
        int count = 0;
        int x = startX + dx;
        int y = startY + dy;
        while (isWithinBounds(x, y)) {
            Stone occupying = stoneAt(x, y);
            if (!occupying.countsForPlayerSequence(playerStone)) {
                break;
            }
            count++;
            x += dx;
            y += dy;
        }
        return count;
    }
}
