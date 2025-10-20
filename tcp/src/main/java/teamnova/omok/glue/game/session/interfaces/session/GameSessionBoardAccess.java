package teamnova.omok.glue.game.session.interfaces.session;

import teamnova.omok.glue.game.session.model.Stone;
import teamnova.omok.glue.game.session.model.vo.StonePlacementMetadata;

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
    StonePlacementMetadata placementAt(int x, int y);
    void recordPlacement(int x, int y, StonePlacementMetadata metadata);
    void clearPlacement(int x, int y);
    void clearPlacements();
    void clear();
    byte[] snapshot();
}
