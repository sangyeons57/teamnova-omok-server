package teamnova.omok.glue.store;

import java.util.Arrays;

/**
 * Holds the raw board state for a single game session using a compact byte array.
 */
public class BoardStore {
    public static final int DEFAULT_WIDTH = 10;
    public static final int DEFAULT_HEIGHT = 10;

    private final int width;
    private final int height;
    private final byte[] cells;

    public BoardStore(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("width and height must be positive");
        }
        this.width = width;
        this.height = height;
        this.cells = new byte[width * height];
        Arrays.fill(cells, Stone.EMPTY.code());
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
}
