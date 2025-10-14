package teamnova.omok.glue.store;

/**
 * Captures mutable turn-related state for a single game session.
 */
public class TurnStore {
    private int currentPlayerIndex = -1;
    private int turnNumber = 0;
    private long turnStartAt;
    private long turnEndAt;

    public int getCurrentPlayerIndex() {
        return currentPlayerIndex;
    }

    public void setCurrentPlayerIndex(int currentPlayerIndex) {
        this.currentPlayerIndex = currentPlayerIndex;
    }

    public int getTurnNumber() {
        return turnNumber;
    }

    public void setTurnNumber(int turnNumber) {
        this.turnNumber = turnNumber;
    }

    public long getTurnStartAt() {
        return turnStartAt;
    }

    public void setTurnStartAt(long turnStartAt) {
        this.turnStartAt = turnStartAt;
    }

    public long getTurnEndAt() {
        return turnEndAt;
    }

    public void setTurnEndAt(long turnEndAt) {
        this.turnEndAt = turnEndAt;
    }
}
