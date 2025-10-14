package teamnova.omok.domain.session.game.entity.board;

import teamnova.omok.domain.session.game.entity.stone.Stone;

import java.util.Arrays;

public class Board implements BoardReadable {
    private final int width;
    private final int height;
    private final byte[] cells;

    private final BoardService boardService;

    public Board(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("width and height must be positive");
        }
        this.width = width;
        this.height = height;
        this.cells = new byte[width * height];
        Arrays.fill(cells, Stone.EMPTY.code());

        this.boardService = new BoardService(this);
    }

    public BoardReadable getReadable() {
        return this;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public byte get(int index) {
        return cells[index];
    }

    public void set(int index, byte value) {
        cells[index] = value;
    }

    public void clear() {
        Arrays.fill(cells, Stone.EMPTY.code());
    }

    public byte[] snapshot() {
        return Arrays.copyOf(cells, cells.length);
    }

    public boolean isWithBounds(int x, int y) {
        return boardService.isWithinBounds(x, y);
    }

    public boolean isEmpty(int x, int y) {
        return boardService.isEmpty(x, y);
    }

    public Stone stoneAt(int x, int y) {
        return boardService.stoneAt(x, y);
    }

    public void setStone(int x, int y, Stone stone) {
        boardService.setStone(x, y, stone);
    }

    public boolean hasFiveInARow(int x, int y, Stone stone) {
        return boardService.hasFiveInARow(x, y, stone);
    }
}
