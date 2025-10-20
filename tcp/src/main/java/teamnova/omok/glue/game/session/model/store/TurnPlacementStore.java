package teamnova.omok.glue.game.session.model.store;

import java.util.Arrays;

import teamnova.omok.glue.game.session.model.vo.StonePlacementMetadata;

/**
 * Stores per-cell placement metadata aligned with the primary board store.
 */
public final class TurnPlacementStore {
    private final int width;
    private final int height;
    private final int size;

    private final int[] turnNumbers;
    private final byte[] playerIndices;
    private final String[] userIds;
    private final StonePlacementMetadata.Source[] sources;

    public TurnPlacementStore(int width, int height) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("width and height must be positive");
        }
        this.width = width;
        this.height = height;
        this.size = width * height;
        this.turnNumbers = new int[size];
        this.playerIndices = new byte[size];
        this.userIds = new String[size];
        this.sources = new StonePlacementMetadata.Source[size];
        Arrays.fill(playerIndices, (byte) -1);
        Arrays.fill(sources, StonePlacementMetadata.Source.SYSTEM);
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }

    public void record(int x, int y, StonePlacementMetadata metadata) {
        record(linearIndex(x, y), metadata);
    }

    public void record(int index, StonePlacementMetadata metadata) {
        validateIndex(index);
        if (metadata == null || metadata.isEmpty()) {
            clear(index);
            return;
        }
        turnNumbers[index] = metadata.turnNumber();
        playerIndices[index] = (byte) metadata.placedByPlayerIndex();
        userIds[index] = metadata.placedByUserId();
        sources[index] = metadata.source();
    }

    public StonePlacementMetadata get(int x, int y) {
        return get(linearIndex(x, y));
    }

    public StonePlacementMetadata get(int index) {
        validateIndex(index);
        int turn = turnNumbers[index];
        if (turn <= 0) {
            return StonePlacementMetadata.empty();
        }
        return new StonePlacementMetadata(
            turn,
            playerIndices[index],
            userIds[index],
            sources[index]
        );
    }

    public void clear(int x, int y) {
        clear(linearIndex(x, y));
    }

    public void clear(int index) {
        validateIndex(index);
        turnNumbers[index] = 0;
        playerIndices[index] = (byte) -1;
        userIds[index] = null;
        sources[index] = StonePlacementMetadata.Source.SYSTEM;
    }

    public void reset() {
        Arrays.fill(turnNumbers, 0);
        Arrays.fill(playerIndices, (byte) -1);
        Arrays.fill(userIds, null);
        Arrays.fill(sources, StonePlacementMetadata.Source.SYSTEM);
    }

    private void validateIndex(int index) {
        if (index < 0 || index >= size) {
            throw new IndexOutOfBoundsException("index " + index + " out of bounds for size " + size);
        }
    }

    private int linearIndex(int x, int y) {
        if (x < 0 || x >= width || y < 0 || y >= height) {
            throw new IndexOutOfBoundsException("Coordinates out of bounds: (" + x + "," + y + ")");
        }
        return y * width + x;
    }
}
