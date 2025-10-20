package teamnova.omok.glue.game.session.interfaces;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionBoardAccess;
import teamnova.omok.glue.game.session.model.Stone;
import teamnova.omok.glue.game.session.model.vo.StonePlacementMetadata;

public interface GameBoardService {
    void reset(GameSessionBoardAccess board);
    boolean isWithinBounds(GameSessionBoardAccess board, int x, int y);
    boolean isEmpty(GameSessionBoardAccess board, int x, int y);
    Stone stoneAt(GameSessionBoardAccess board, int x, int y);
    void setStone(GameSessionBoardAccess board, int x, int y, Stone stone, StonePlacementMetadata metadata);
    StonePlacementMetadata placementAt(GameSessionBoardAccess board, int x, int y);
    byte[] snapshot(GameSessionBoardAccess board);
    boolean hasFiveInARow(GameSessionBoardAccess board, int x, int y, Stone stone);
}
