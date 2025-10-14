package teamnova.omok.domain.session.game.entity.board;

import teamnova.omok.domain.session.game.entity.stone.Stone;

public interface BoardReadable {
    public int width();

    public int height();

    public byte get(int index);

    public byte[] snapshot();

    public boolean isWithBounds(int x, int y);

    public boolean isEmpty(int x, int y);

    public Stone stoneAt(int x, int y);

    public boolean hasFiveInARow(int x, int y, Stone stone);
}
