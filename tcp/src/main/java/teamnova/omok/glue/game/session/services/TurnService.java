package teamnova.omok.glue.game.session.services;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionTurnAccess;
import teamnova.omok.glue.game.session.interfaces.GameTurnService;
import teamnova.omok.glue.game.session.interfaces.TurnAdvanceStrategy;
import teamnova.omok.glue.game.session.model.vo.TurnCounters;
import teamnova.omok.glue.game.session.model.vo.TurnOrder;
import teamnova.omok.glue.game.session.model.vo.TurnTiming;

/**
 * Handles turn sequencing logic for in-game sessions.
 */
public class TurnService implements GameTurnService {
    private final long durationMillis;
    private final TurnAdvanceStrategy advanceStrategy;

    public TurnService(long durationMillis) {
        this(durationMillis, new SequentialTurnAdvanceStrategy());
    }

    public TurnService(long durationMillis, TurnAdvanceStrategy advanceStrategy) {
        if (durationMillis <= 0) {
            throw new IllegalArgumentException("durationMillis must be positive");
        }
        this.durationMillis = durationMillis;
        this.advanceStrategy = Objects.requireNonNull(advanceStrategy, "advanceStrategy");
    }

    @Override
    public TurnSnapshot start(GameSessionTurnAccess store, List<String> userOrder, long now) {
        requireStore(store);
        requirePlayers(userOrder);
        TurnOrder order = TurnOrder.of(userOrder);
        store.order(order);
        store.setCurrentPlayerIndex(0);
        store.counters(TurnCounters.first());
        store.timing(TurnTiming.of(now, now + durationMillis));
        return snapshot(store);
    }

    @Override
    public TurnSnapshot advanceSkippingDisconnected(GameSessionTurnAccess store,
                                                    Set<String> disconnectedUserIds,
                                                    long now) {
        requireStore(store);
        TurnOrder order = ensureOrder(store);
        int current = Math.max(-1, store.getCurrentPlayerIndex());
        Optional<TurnAdvanceStrategy.Result> candidate =
            advanceStrategy.next(order, current, disconnectedUserIds);
        if (candidate.isEmpty()) {
            store.setCurrentPlayerIndex(-1);
            store.counters(store.counters().advanceWithoutActive());
            store.timing(TurnTiming.of(now, now + durationMillis));
            return snapshot(store);
        }
        TurnAdvanceStrategy.Result result = candidate.get();
        store.setCurrentPlayerIndex(result.nextIndex());
        store.counters(store.counters().advance(result.wrapped(), order.size()));
        store.timing(TurnTiming.of(now, now + durationMillis));
        return snapshot(store);
    }

    @Override
    public boolean isExpired(GameSessionTurnAccess store, long now) {
        requireStore(store);
        return store.timing().isExpired(now);
    }

    @Override
    public Integer currentPlayerIndex(GameSessionTurnAccess store) {
        requireStore(store);
        int index = store.getCurrentPlayerIndex();
        return index >= 0 ? index : null;
    }

    @Override
    public TurnSnapshot snapshot(GameSessionTurnAccess store) {
        requireStore(store);
        TurnOrder order = store.order();
        int index = store.getCurrentPlayerIndex();
        String playerId = null;
        if (order != null && index >= 0 && index < order.size()) {
            playerId = order.userIdAt(index);
        }
        return new TurnSnapshot(
            index,
            playerId,
            store.counters(),
            store.timing(),
            order
        );
    }

    private TurnOrder ensureOrder(GameSessionTurnAccess store) {
        TurnOrder order = store.order();
        if (order == null || order.size() == 0) {
            throw new IllegalStateException("Turn order has not been initialized");
        }
        return order;
    }

    private void requirePlayers(List<String> userOrder) {
        if (userOrder == null || userOrder.isEmpty()) {
            throw new IllegalStateException("No players registered for turn management");
        }
    }

    private void requireStore(GameSessionTurnAccess store) {
        if (store == null) {
            throw new IllegalArgumentException("store must not be null");
        }
    }
}
