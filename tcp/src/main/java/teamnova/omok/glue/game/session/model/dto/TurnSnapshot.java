package teamnova.omok.glue.game.session.model.dto;

import teamnova.omok.glue.game.session.model.vo.TurnCounters;
import teamnova.omok.glue.game.session.model.vo.TurnTiming;

public record TurnSnapshot(int currentPlayerIndex,
                    String currentPlayerId,
                    TurnCounters counters,
                    TurnTiming timing,
                    boolean wrapped) {
    public int actionNumber() {
        return counters.actionNumber();
    }
    public int turnNumber() {
        return actionNumber();
    }
    public int roundNumber() {
        return counters.roundNumber();
    }
    public int positionInRound() {
        return counters.positionInRound();
    }
    public long turnStartAt() {
        return timing.startAt();
    }
    public long turnEndAt() {
        return timing.endAt();
    }
}