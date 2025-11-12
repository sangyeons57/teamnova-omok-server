package teamnova.omok.support;

import java.util.Arrays;
import java.util.concurrent.locks.ReentrantLock;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionBoardAccess;
import teamnova.omok.glue.game.session.model.Stone;
import teamnova.omok.glue.game.session.model.vo.GameSessionId;
import teamnova.omok.glue.game.session.model.vo.StonePlacementMetadata;

public final class TestBoardAccess implements GameSessionBoardAccess {
    private final GameSessionId sessionId = GameSessionId.random();
    private final int width;
    private final int height;
    private final ReentrantLock lock = new ReentrantLock();
    private final Stone[][] cells;
    private final StonePlacementMetadata[][] placements;

    public TestBoardAccess(int width, int height) {
        this.width = width;
        this.height = height;
        this.cells = new Stone[height][width];
        this.placements = new StonePlacementMetadata[height][width];
        clear();
    }

    public void putStone(int x, int y, Stone stone) {
        cells[y][x] = stone;
    }

    @Override
    public GameSessionId sessionId() {
        return sessionId;
    }

    @Override
    public ReentrantLock lock() {
        return lock;
    }

    @Override
    public int width() {
        return width;
    }

    @Override
    public int height() {
        return height;
    }

    @Override
    public boolean isWithinBounds(int x, int y) {
        return x >= 0 && x < width && y >= 0 && y < height;
    }

    @Override
    public boolean isEmpty(int x, int y) {
        return stoneAt(x, y) == Stone.EMPTY;
    }

    @Override
    public Stone stoneAt(int x, int y) {
        return cells[y][x];
    }

    @Override
    public void setStone(int x, int y, Stone stone) {
        cells[y][x] = stone;
    }

    @Override
    public StonePlacementMetadata placementAt(int x, int y) {
        return placements[y][x];
    }

    @Override
    public void recordPlacement(int x, int y, StonePlacementMetadata metadata) {
        placements[y][x] = metadata;
    }

    @Override
    public void clearPlacement(int x, int y) {
        placements[y][x] = StonePlacementMetadata.empty();
    }

    @Override
    public void clearPlacements() {
        for (StonePlacementMetadata[] row : placements) {
            Arrays.fill(row, StonePlacementMetadata.empty());
        }
    }

    @Override
    public void clear() {
        for (Stone[] row : cells) {
            Arrays.fill(row, Stone.EMPTY);
        }
        clearPlacements();
    }

    @Override
    public byte[] snapshot() {
        byte[] snapshot = new byte[width * height];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                snapshot[y * width + x] = stoneAt(x, y).code();
            }
        }
        return snapshot;
    }
}
