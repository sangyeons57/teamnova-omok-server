package teamnova.omok.glue.game.session.model.runtime;

import teamnova.omok.glue.game.session.model.dto.TurnSnapshot;
import teamnova.omok.glue.game.session.model.Stone;
import teamnova.omok.glue.game.session.model.result.MoveStatus;

/**
 * Captures immutable metadata for a single player's turn within an overall turn cycle.
 */
public final class TurnPersonalFrame {

    private final long startedAt;
    private boolean moveActive;
    private String userId;
    private int originalX;
    private int originalY;
    private int targetX;
    private int targetY;
    private long requestedAtMillis;
    private Stone stone;

    private MoveStatus outcomeStatus;
    private boolean outcomeConsumed;

    private boolean timeoutResolved;
    private boolean timeoutTimedOut;
    private String timeoutPreviousPlayerId;
    private boolean timeoutConsumed;

    private TurnSnapshot currentSnapshot;
    private TurnSnapshot nextSnapshot;
    private TurnSnapshot timeoutSnapshot;

    private TurnPersonalFrame(long startedAt) {
        this.startedAt = startedAt;
        clearTimeoutOutcome();
    }

    public static TurnPersonalFrame fromSnapshot(TurnSnapshot snapshot,
                                                 long startedAt) {
        TurnPersonalFrame frame = new TurnPersonalFrame(startedAt);
        if (snapshot != null) {
            frame.currentSnapshot = snapshot;
        }
        return frame;
    }

    public long startedAt() {
        return startedAt;
    }

    public boolean hasActiveMove() {
        return moveActive;
    }

    public void beginMove(String userId, int x, int y, long requestedAtMillis) {
        this.moveActive = true;
        this.userId = userId;
        this.originalX = x;
        this.originalY = y;
        this.targetX = x;
        this.targetY = y;
        this.requestedAtMillis = requestedAtMillis;
        this.stone = null;
        this.currentSnapshot = null;
        this.nextSnapshot = null;
        this.outcomeStatus = null;
        this.outcomeConsumed = false;
        clearTimeoutOutcome();
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

    public long requestedAtMillis() {
        return requestedAtMillis;
    }

    public Stone stone() {
        return stone;
    }

    public void stone(Stone stone) {
        this.stone = stone;
    }

    public TurnSnapshot currentSnapshot() {
        return currentSnapshot;
    }

    public void currentSnapshot(TurnSnapshot snapshot) {
        this.currentSnapshot = snapshot;
    }

    public TurnSnapshot nextSnapshot() {
        return nextSnapshot;
    }

    public void nextSnapshot(TurnSnapshot snapshot) {
        this.nextSnapshot = snapshot;
    }

    public MoveStatus outcomeStatus() {
        return outcomeStatus;
    }

    public void resolveOutcome(MoveStatus status) {
        this.moveActive = false;
        this.outcomeStatus = status;
        this.outcomeConsumed = false;
    }

    public void endActiveMove() {
        this.moveActive = false;
    }

    public boolean hasPendingOutcome() {
        return outcomeStatus != null && !outcomeConsumed;
    }

    public void markOutcomeConsumed() {
        this.outcomeConsumed = true;
    }

    public TurnSnapshot outcomeSnapshot() {
        if (outcomeStatus == MoveStatus.SUCCESS) {
            return nextSnapshot;
        }
        return currentSnapshot;
    }

    public void resolveTimeout(boolean timedOut, TurnSnapshot snapshot, String previousPlayerId) {
        this.timeoutResolved = true;
        this.timeoutTimedOut = timedOut;
        this.timeoutSnapshot = snapshot;
        this.timeoutPreviousPlayerId = previousPlayerId;
        this.timeoutConsumed = false;
    }

    public boolean hasTimeoutOutcome() {
        return timeoutResolved;
    }

    public boolean timeoutTimedOut() {
        return timeoutResolved && timeoutTimedOut;
    }

    public TurnSnapshot timeoutSnapshot() {
        return timeoutSnapshot;
    }

    public String timeoutPreviousPlayerId() {
        return timeoutPreviousPlayerId;
    }

    public void markTimeoutConsumed() {
        this.timeoutConsumed = true;
    }

    public void clearTimeoutOutcome() {
        this.timeoutResolved = false;
        this.timeoutTimedOut = false;
        this.timeoutSnapshot = null;
        this.timeoutPreviousPlayerId = null;
        this.timeoutConsumed = false;
    }
}
