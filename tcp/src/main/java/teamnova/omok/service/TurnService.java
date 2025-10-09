package teamnova.omok.service;

import teamnova.omok.store.TurnStore;

import java.util.List;

/**
 * Handles turn sequencing logic for in-game sessions.
 */
public class TurnService {
    private final long durationMillis;

    public TurnService(long durationMillis) {
        if (durationMillis <= 0) {
            throw new IllegalArgumentException("durationMillis must be positive");
        }
        this.durationMillis = durationMillis;
    }

    public TurnSnapshot start(TurnStore store, List<String> userOrder, long now) {
        requirePlayers(userOrder);
        store.setCurrentPlayerIndex(0);
        store.setTurnNumber(1);
        store.setTurnStartAt(now);
        store.setTurnEndAt(now + durationMillis);
        return snapshot(store, userOrder);
    }

    public TurnSnapshot advance(TurnStore store, List<String> userOrder, long now) {
        requirePlayers(userOrder);
        int current = store.getCurrentPlayerIndex();
        if (current < 0) {
            store.setCurrentPlayerIndex(0);
        } else {
            store.setCurrentPlayerIndex((current + 1) % userOrder.size());
        }
        store.setTurnNumber(Math.max(1, store.getTurnNumber() + 1));
        store.setTurnStartAt(now);
        store.setTurnEndAt(now + durationMillis);
        return snapshot(store, userOrder);
    }

    public boolean isExpired(TurnStore store, long now) {
        return store.getTurnEndAt() > 0 && now >= store.getTurnEndAt();
    }

    public Integer currentPlayerIndex(TurnStore store) {
        int index = store.getCurrentPlayerIndex();
        return index >= 0 ? index : null;
    }

    public TurnSnapshot snapshot(TurnStore store, List<String> userOrder) {
        requirePlayers(userOrder);
        int index = store.getCurrentPlayerIndex();
        String playerId = (index >= 0 && index < userOrder.size()) ? userOrder.get(index) : null;
        return new TurnSnapshot(index, playerId, store.getTurnNumber(), store.getTurnStartAt(), store.getTurnEndAt());
    }

    private void requirePlayers(List<String> userOrder) {
        if (userOrder == null || userOrder.isEmpty()) {
            throw new IllegalStateException("No players registered for turn management");
        }
    }

    public record TurnSnapshot(int currentPlayerIndex, String currentPlayerId, int turnNumber,
                               long turnStartAt, long turnEndAt) { }
}
