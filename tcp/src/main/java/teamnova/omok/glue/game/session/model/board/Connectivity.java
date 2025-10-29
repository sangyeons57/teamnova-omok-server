package teamnova.omok.glue.game.session.model.board;

/**
 * Connectivity model for board traversals.
 */
public enum Connectivity {
    FOUR_WAY(new int[][]{
        {1, 0}, {-1, 0}, {0, 1}, {0, -1}
    }),
    EIGHT_WAY(new int[][]{
        {1, 0}, {-1, 0}, {0, 1}, {0, -1},
        {1, 1}, {-1, -1}, {1, -1}, {-1, 1}
    });

    private final int[][] offsets;

    Connectivity(int[][] offsets) {
        this.offsets = offsets;
    }

    public int[][] offsets() {
        return offsets;
    }
}
