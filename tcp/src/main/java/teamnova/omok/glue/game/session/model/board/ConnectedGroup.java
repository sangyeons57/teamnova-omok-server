package teamnova.omok.glue.game.session.model.board;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import teamnova.omok.glue.game.session.model.Stone;

/**
 * Represents a connected set of stones on the board.
 */
public final class ConnectedGroup {
    private final Stone stone;
    private final List<BoardPoint> points;

    public ConnectedGroup(Stone stone, List<BoardPoint> points) {
        this.stone = Objects.requireNonNull(stone, "stone");
        this.points = List.copyOf(Objects.requireNonNull(points, "points"));
    }

    public Stone stone() {
        return stone;
    }

    public List<BoardPoint> points() {
        return Collections.unmodifiableList(points);
    }

    public int size() {
        return points.size();
    }
}
