package teamnova.omok.domain.session.game.entity.state.manage;

import teamnova.omok.domain.session.game.GameSession;
import teamnova.omok.domain.session.game.entity.stone.Stone;
import teamnova.omok.domain.session.game.entity.turn.Turn;
import teamnova.omok.domain.session.game.entity.turn.TurnSnapshot;

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

    public Turn turnStore() {
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
        private TurnSnapshot current;
        private TurnSnapshot next;

        public TurnSnapshot current() {
            return current;
        }

        public void current(TurnSnapshot snapshot) {
            this.current = snapshot;
        }

        public TurnSnapshot next() {
            return next;
        }

        public void next(TurnSnapshot snapshot) {
            this.next = snapshot;
        }
    }
}
