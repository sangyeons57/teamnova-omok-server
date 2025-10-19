package teamnova.omok.glue.game.session.interfaces.session;

import teamnova.omok.glue.game.session.model.Stone;

/**
 * Accessor contract exposed by {@code GameSession} for board state interactions.
 */

public interface GameSessionBoardAccess extends GameSessionAccessInterface {
    int width();
    int height();
    boolean isWithinBounds(int x, int y);
    boolean isEmpty(int x, int y);
    Stone stoneAt(int x, int y);
    void setStone(int x, int y, Stone stone);
    void clear();
    byte[] snapshot();
}
