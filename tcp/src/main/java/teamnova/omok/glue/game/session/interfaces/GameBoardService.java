package teamnova.omok.glue.game.session.interfaces;

import teamnova.omok.glue.game.session.model.BoardStore;
import teamnova.omok.glue.game.session.model.Stone;

public interface GameBoardService {
    void reset(BoardStore store);
    boolean isWithinBounds(BoardStore store, int x, int y);
    boolean isEmpty(BoardStore store, int x, int y);
    Stone stoneAt(BoardStore store, int x, int y);
    void setStone(BoardStore store, int x, int y, Stone stone);
    byte[] snapshot(BoardStore store);
    boolean hasFiveInARow(BoardStore store, int x, int y, Stone stone);
}
