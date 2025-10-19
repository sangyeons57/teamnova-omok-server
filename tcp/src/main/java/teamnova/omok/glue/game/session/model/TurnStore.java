package teamnova.omok.glue.game.session.model;

import java.util.Objects;

import teamnova.omok.glue.game.session.model.vo.TurnCounters;
import teamnova.omok.glue.game.session.model.vo.TurnOrder;
import teamnova.omok.glue.game.session.model.vo.TurnTiming;

/**
 * Captures mutable turn-related state for a single game session.
 */
public class TurnStore {
    private TurnOrder order = TurnOrder.empty();
    private TurnCounters counters = TurnCounters.initial();
    private int currentPlayerIndex = -1;
    private TurnTiming timing = TurnTiming.idle();

    public TurnOrder order() {
        return order;
    }

    public void order(TurnOrder order) {
        this.order = Objects.requireNonNull(order, "order");
    }

    public TurnCounters counters() {
        return counters;
    }

    public void counters(TurnCounters counters) {
        this.counters = Objects.requireNonNull(counters, "counters");
    }

    public int actionNumber() {
        return counters.actionNumber();
    }

    public int getCurrentPlayerIndex() {
        return currentPlayerIndex;
    }

    public void setCurrentPlayerIndex(int currentPlayerIndex) {
        this.currentPlayerIndex = currentPlayerIndex;
    }

    public TurnTiming timing() {
        return timing;
    }

    public void timing(TurnTiming timing) {
        this.timing = Objects.requireNonNull(timing, "timing");
    }

    public void reset() {
        this.order = TurnOrder.empty();
        this.counters = TurnCounters.initial();
        this.currentPlayerIndex = -1;
        this.timing = TurnTiming.idle();
    }
}
