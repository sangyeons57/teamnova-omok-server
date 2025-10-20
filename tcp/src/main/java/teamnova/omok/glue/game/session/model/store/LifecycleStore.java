package teamnova.omok.glue.game.session.model.store;

/**
 * Captures lifecycle timestamps and counters for a game session.
 */
public final class LifecycleStore {
    private final long createdAt;

    private volatile boolean gameStarted;
    private volatile long gameStartedAt;
    private volatile long gameEndedAt;
    private volatile int completedTurnCount;

    public LifecycleStore(long createdAt) {
        this.createdAt = createdAt;
    }

    public long createdAt() {
        return createdAt;
    }

    public boolean gameStarted() {
        return gameStarted;
    }

    public long gameStartedAt() {
        return gameStartedAt;
    }

    public long gameEndedAt() {
        return gameEndedAt;
    }

    public int completedTurnCount() {
        return completedTurnCount;
    }

    public long gameDurationMillis() {
        long start = gameStartedAt;
        long end = gameEndedAt;
        if (start <= 0 || end <= 0 || end < start) {
            return 0L;
        }
        return end - start;
    }

    public void markGameStarted(long startedAt) {
        this.gameStarted = true;
        this.gameStartedAt = startedAt;
        this.gameEndedAt = 0L;
        this.completedTurnCount = 0;
    }

    public void markGameFinished(long endedAt, int turnCount) {
        if (endedAt > 0 && (this.gameEndedAt == 0L || endedAt > this.gameEndedAt)) {
            this.gameEndedAt = endedAt;
        }
        if (turnCount > 0 && turnCount > this.completedTurnCount) {
            this.completedTurnCount = turnCount;
        }
    }
}
