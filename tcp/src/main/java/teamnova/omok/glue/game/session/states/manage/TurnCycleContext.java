package teamnova.omok.glue.game.session.states.manage;

import teamnova.omok.glue.game.session.interfaces.GameTurnService;
import teamnova.omok.glue.game.session.model.GameSession;
import teamnova.omok.glue.game.session.model.Stone;
import teamnova.omok.glue.game.session.model.TurnStore;

/**
 * Mutable data shared across turn-processing phases.
 */
public final class TurnCycleContext {
    private final GameSessionStateContext parent;
    private final String userId;
    private final int x;
    private final int y;
    private final long now;

    private Stone stone;
    private final TurnServiceSnapshotWrapper snapshots = new TurnServiceSnapshotWrapper();

    public TurnCycleContext(GameSessionStateContext parent, String userId, int x, int y, long now) {
        this.parent = parent;
        this.userId = userId;
        this.x = x;
        this.y = y;
        this.now = now;
    }

    public GameSession session() {
        return parent.session();
    }

    public String userId() {
        return userId;
    }

    public int x() {
        return x;
    }

    public int y() {
        return y;
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
