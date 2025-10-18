package teamnova.omok.glue.game.session.interfaces;

import java.util.List;
import java.util.Set;

import teamnova.omok.glue.game.session.model.TurnStore;

public interface GameTurnService {
    TurnSnapshot start(TurnStore store, List<String> userOrder, long now);
    TurnSnapshot advanceSkippingDisconnected(TurnStore store,
                                             List<String> userOrder,
                                             Set<String> disconnectedUserIds,
                                             long now);
    boolean isExpired(TurnStore store, long now);
    Integer currentPlayerIndex(TurnStore store);
    TurnSnapshot snapshot(TurnStore store, List<String> userOrder);

    record TurnSnapshot(int currentPlayerIndex,
                        String currentPlayerId,
                        int turnNumber,
                        long turnStartAt,
                        long turnEndAt) { }
}
