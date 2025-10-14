package teamnova.omok.glue.state.game.manage;

import teamnova.omok.glue.service.TurnService;
import teamnova.omok.glue.store.GameSession;
import teamnova.omok.glue.store.Stone;
import teamnova.omok.glue.store.TurnStore;

/**
 * Mutable data shared across turn-processing phases.
 */
public final class TurnCycleContext {
    private final GameSessionStateContext parent;
    private final String userId;
    private final int x;
    private final int y;
    private final long now;

    private int playerIndex = -1;
    private Stone stone;
    private TurnServiceSnapshotWrapper snapshots = new TurnServiceSnapshotWrapper();

    public TurnCycleContext(GameSessionStateContext parent, String userId, int x, int y, long now) {
        this.parent = parent;
        this.userId = userId;
        this.x = x;
        this.y = y;
        this.now = now;
    }

    GameSessionStateContext parent() {
        return parent;
    }

    public GameSession session() {
        return parent.session();
    }

    public TurnStore turnStore() {
        return parent.session().getTurnStore();
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

    public int playerIndex() {
        return playerIndex;
    }

    public void playerIndex(int playerIndex) {
        this.playerIndex = playerIndex;
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
        private TurnService.TurnSnapshot current;
        private TurnService.TurnSnapshot next;

        public TurnService.TurnSnapshot current() {
            return current;
        }

        public void current(TurnService.TurnSnapshot snapshot) {
            this.current = snapshot;
        }

        public TurnService.TurnSnapshot next() {
            return next;
        }

        public void next(TurnService.TurnSnapshot snapshot) {
            this.next = snapshot;
        }
    }
}
