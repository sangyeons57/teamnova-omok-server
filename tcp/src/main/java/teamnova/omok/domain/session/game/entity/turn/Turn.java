package teamnova.omok.domain.session.game.entity.turn;

import java.util.List;
import java.util.Set;

/**
 * Captures mutable turn-related state for a single game session.
 */
public class Turn implements TurnReadable{
    public static final long TURN_DURATION_MILLIS = 15_000L;

    private int currentPlayerIndex = -1;
    private int turnNumber = 0;
    private long turnStartAt;
    private long turnEndAt;

    private final TurnService turnService;

    public Turn() {
        this.turnService = new TurnService(this, TURN_DURATION_MILLIS);
    }

    public TurnReadable getReadable() {
        return this;
    }

    public TurnSnapshot start(List<String> userOrder, long now) {
        return turnService.start(userOrder, now);
    }

    public TurnSnapshot snapshot(List<String> userOrder) {
        return turnService.snapshot(userOrder);
    }

    public TurnSnapshot advanceSkippingDisconnected(List<String> userOrder, Set<String> disconnectedUserIds, long now) {
        return turnService.advanceSkippingDisconnected(userOrder, disconnectedUserIds, now);
    }

    public boolean isExpired(long now) {
        return turnService.isExpired(now);
    }

    public Integer currentPlayerIndex() {
        return turnService.currentPlayerIndex();
    }

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
