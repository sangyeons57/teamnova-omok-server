package teamnova.omok.domain.session.game.entity.turn;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class TurnService {
    private final Turn turn;
    private final long durationMillis;

    public TurnService(Turn turn, long durationMillis) {
        TurnGuard.durationsGuard(durationMillis);

        this.durationMillis = durationMillis;
        this.turn = turn;
    }

    public TurnSnapshot start(List<String> userOrder, long now) {
        TurnGuard.requirePlayersGuard(userOrder);
        turn.setCurrentPlayerIndex(0);
        turn.setTurnNumber(1);
        turn.setTurnStartAt(now);
        turn.setTurnEndAt(now + durationMillis);
        return snapshot(userOrder);
    }

    public TurnSnapshot advanceSkippingDisconnected( List<String> userOrder,
                                                    Set<String> disconnectedUserIds,
                                                    long now) {
        TurnGuard.requirePlayersGuard(userOrder);
        Set<String> skip = (disconnectedUserIds == null) ? Collections.emptySet() : disconnectedUserIds;
        int total = userOrder.size();
        int current = Math.max(-1, turn.getCurrentPlayerIndex());
        int checked = 0;
        int nextIndex = current;
        while (checked < total) {
            nextIndex = (nextIndex + 1) % total;
            String candidate = userOrder.get(nextIndex);
            if (!skip.contains(candidate)) {
                turn.setCurrentPlayerIndex(nextIndex);
                updateTurnTiming(turn, now);
                return snapshot(userOrder);
            }
            checked++;
        }
        turn.setCurrentPlayerIndex(-1);
        updateTurnTiming(turn, now);
        return snapshot(userOrder);
    }

    public boolean isExpired(long now) {
        return turn.getTurnEndAt() > 0 && now >= turn.getTurnEndAt();
    }

    public Integer currentPlayerIndex() {
        int index = turn.getCurrentPlayerIndex();
        return index >= 0 ? index : null;
    }

    public TurnSnapshot snapshot(List<String> userOrder) {
        TurnGuard.requirePlayersGuard(userOrder);
        int index = turn.getCurrentPlayerIndex();
        String playerId = (index >= 0 && index < userOrder.size()) ? userOrder.get(index) : null;
        return new TurnSnapshot(index, playerId, turn.getTurnNumber(), turn.getTurnStartAt(), turn.getTurnEndAt());
    }



    private void updateTurnTiming(Turn store, long now) {
        store.setTurnNumber(Math.max(1, store.getTurnNumber() + 1));
        store.setTurnStartAt(now);
        store.setTurnEndAt(now + durationMillis);
    }

}
