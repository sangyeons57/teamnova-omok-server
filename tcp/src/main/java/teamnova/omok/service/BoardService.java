package teamnova.omok.service;

import teamnova.omok.store.BoardStore;
import teamnova.omok.store.Stone;

/**
 * Stateless utility service for manipulating Omok board data.
 */
public class BoardService {
    public void reset(BoardStore store) {
        store.clear();
    }

    public boolean isWithinBounds(BoardStore store, int x, int y) {
        return x >= 0 && x < store.width() && y >= 0 && y < store.height();
    }

    public boolean isEmpty(BoardStore store, int x, int y) {
        return stoneAt(store, x, y) == Stone.EMPTY;
    }

    public Stone stoneAt(BoardStore store, int x, int y) {
        return Stone.fromByte(store.get(linearIndex(store, x, y)));
    }

    public void setStone(BoardStore store, int x, int y, Stone stone) {
        store.set(linearIndex(store, x, y), stone.code());
    }

    public byte[] snapshot(BoardStore store) {
        return store.snapshot();
    }

    private int linearIndex(BoardStore store, int x, int y) {
        return y * store.width() + x;
    }
}
