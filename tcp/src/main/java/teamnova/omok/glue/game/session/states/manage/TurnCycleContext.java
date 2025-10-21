package teamnova.omok.glue.game.session.states.manage;

import teamnova.omok.glue.game.session.interfaces.GameTurnService;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionAccess;
import teamnova.omok.glue.game.session.model.Stone;

/**
 * Mutable data shared across turn-processing phases.
 */
public final class TurnCycleContext {
    private final String userId;
    private final int originalX;
    private final int originalY;
    private final long now;

    private int targetX;
    private int targetY;
    private Stone stone;
    private final TurnServiceSnapshotWrapper snapshots = new TurnServiceSnapshotWrapper();

    public TurnCycleContext(String userId, int x, int y, long now) {
        this.userId = userId;
        this.originalX = x;
        this.originalY = y;
        this.now = now;
        this.targetX = x;
        this.targetY = y;
    }

    public String userId() {
        return userId;
    }

    public int x() {
        return targetX;
    }

    public int y() {
        return targetY;
    }

    public int originalX() {
        return originalX;
    }

    public int originalY() {
        return originalY;
    }

    public void updatePosition(int x, int y) {
        this.targetX = x;
        this.targetY = y;
    }

    public long now() {
        return now;
    }

    public Stone stone() {
        return stone;
    }

    public void stone(Stone stone) {
        this.stone = stone;
    }

    public TurnServiceSnapshotWrapper snapshots() {
        return snapshots;
    }

    public static final class TurnServiceSnapshotWrapper {
        private GameTurnService.TurnSnapshot current;
        private GameTurnService.TurnSnapshot next;

        public GameTurnService.TurnSnapshot current() {
            return current;
        }

        public void current(GameTurnService.TurnSnapshot snapshot) {
            this.current = snapshot;
        }

        public GameTurnService.TurnSnapshot next() {
            return next;
        }

        public void next(GameTurnService.TurnSnapshot snapshot) {
            this.next = snapshot;
        }

    }
}
