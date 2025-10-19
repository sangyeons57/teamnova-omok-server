package teamnova.omok.glue.game.session.interfaces;

import java.util.List;
import java.util.Set;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionTurnAccess;
import teamnova.omok.glue.game.session.model.vo.TurnCounters;
import teamnova.omok.glue.game.session.model.vo.TurnOrder;
import teamnova.omok.glue.game.session.model.vo.TurnTiming;

public interface GameTurnService {
    TurnSnapshot start(GameSessionTurnAccess turns, List<String> userOrder, long now);
    TurnSnapshot advanceSkippingDisconnected(GameSessionTurnAccess turns,
                                             Set<String> disconnectedUserIds,
                                             long now);
    boolean isExpired(GameSessionTurnAccess turns, long now);
    Integer currentPlayerIndex(GameSessionTurnAccess turns);
    TurnSnapshot snapshot(GameSessionTurnAccess turns);

    record TurnSnapshot(int currentPlayerIndex,
                        String currentPlayerId,
                        TurnCounters counters,
                        TurnTiming timing,
                        TurnOrder order) {
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
}
