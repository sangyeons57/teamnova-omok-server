package teamnova.omok.glue.game.session.services;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import teamnova.omok.glue.game.session.interfaces.GameTurnService;
import teamnova.omok.glue.game.session.interfaces.TurnAdvanceStrategy;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionTurnAccess;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionTurnRuntimeAccess;
import teamnova.omok.glue.game.session.model.dto.TurnSnapshot;
import teamnova.omok.glue.game.session.model.runtime.TurnPersonalFrame;
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
        long resolvedDuration = resolveDurationMillis(store);
        store.timing(TurnTiming.of(now, now + resolvedDuration));
        System.out.println("[TurnService] start order=" + order.userIds()
            + " duration=" + resolvedDuration + " currentIndex=0");
        return snapshot(store);
    }

    @Override
    public TurnSnapshot advance(GameSessionTurnAccess store, long now) {
        requireStore(store);
        TurnOrder order = ensureOrder(store);
        int current = Math.max(-1, store.getCurrentPlayerIndex());
        TurnCounters counters = store.counters();
        if (counters == null) {
            counters = TurnCounters.first();
            store.counters(counters);
        }
        int currentRound = Math.max(1, counters.roundNumber());
        String currentPlayerId = null;
        if (order != null && current >= 0 && current < order.size()) {
            currentPlayerId = order.userIdAt(current);
        }
        boolean roundComplete = isRoundComplete(store, order, currentRound, currentPlayerId);
        System.out.println("[TurnService] advance state order=" + order.userIds()
            + " currentIndex=" + current
            + " round=" + currentRound
            + " actingPlayer=" + currentPlayerId
            + " roundComplete=" + roundComplete);
        Optional<TurnAdvanceStrategy.Result> candidate =
            advanceStrategy.next(order, current);
        if (candidate.isEmpty()) {
            store.setCurrentPlayerIndex(-1);
            TurnCounters updatedCounters;
            if (roundComplete) {
                updatedCounters = counters.advance(true, order.size());
                store.counters(updatedCounters);
            } else {
                updatedCounters = counters.advanceWithoutActive();
                store.counters(updatedCounters);
            }
            long resolvedDuration = resolveDurationMillis(store);
            store.timing(TurnTiming.of(now, now + resolvedDuration));
            System.out.println("[TurnService] advance no candidate roundComplete=" + roundComplete
                + " counters=" + store.counters()
                + " timing=" + store.timing());
            return snapshot(store, roundComplete);
        }
        TurnAdvanceStrategy.Result result = candidate.get();
        store.setCurrentPlayerIndex(result.nextIndex());
        boolean wrapped = roundComplete;
        TurnCounters updatedCounters = counters.advance(wrapped, order.size());
        store.counters(updatedCounters);
        long resolvedDuration = resolveDurationMillis(store);
        store.timing(TurnTiming.of(now, now + resolvedDuration));
        System.out.println("[TurnService] advance nextIndex=" + result.nextIndex()
            + " wrapped=" + wrapped
            + " counters=" + updatedCounters
            + " timing=" + store.timing());
        return snapshot(store, wrapped);
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
        return snapshot(store, false);
    }

    @Override
    public TurnSnapshot reseedOrder(GameSessionTurnAccess store,
                                    List<String> newOrder,
                                    long now) {
        requireStore(store);
        requirePlayers(newOrder);

        TurnOrder previousOrder = store.order();
        List<String> previousOrderIds = Optional.ofNullable(previousOrder)
            .map(TurnOrder::userIds)
            .orElse(List.of());
        TurnOrder updatedOrder = TurnOrder.of(newOrder);
        store.order(updatedOrder);

        int targetIndex = updatedOrder.size() > 0 ? 0 : -1;
        store.setCurrentPlayerIndex(targetIndex);

        TurnCounters counters = store.counters();
        int actionNumber = counters != null ? Math.max(1, counters.actionNumber()) : 1;
        int roundNumber = counters != null ? Math.max(1, counters.roundNumber()) : 1;
        int positionInRound = updatedOrder.size() <= 0 ? 0 : 1;
        store.counters(new TurnCounters(actionNumber, roundNumber, positionInRound));
        long resolvedDuration = resolveDurationMillis(store);
        store.timing(TurnTiming.of(now, now + resolvedDuration));
        System.out.println("[TurnService] reseed previousOrder=" + previousOrderIds
            + " newOrder=" + updatedOrder.userIds()
            + " targetIndex=" + targetIndex
            + " counters=" + store.counters()
            + " timing=" + store.timing());
        return snapshot(store);
    }

    private TurnSnapshot snapshot(GameSessionTurnAccess store, boolean wrapped) {
        requireStore(store);
        TurnOrder order = store.order();
        int index = store.getCurrentPlayerIndex();
        String playerId = null;
        if (order != null && index >= 0 && index < order.size()) {
            playerId = order.userIdAt(index);
        }
        System.out.println("[TurnService] snapshot order=" + (order != null ? order.userIds() : List.of())
            + " index=" + index + " playerId=" + playerId + " wrapped=" + wrapped);
        return new TurnSnapshot(
            index,
            playerId,
            store.counters(),
            store.timing(),
            wrapped
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

    private long resolveDurationMillis(GameSessionTurnAccess store) {
        long override = store.durationMillis();
        return override > 0 ? override : durationMillis;
    }

    private boolean isRoundComplete(GameSessionTurnAccess store,
                                    TurnOrder order,
                                    int roundNumber,
                                    String lastActorId) {
        if (order == null) {
            return false;
        }
        List<String> orderUserIds = order.userIds();
        if (orderUserIds.isEmpty()) {
            return false;
        }
        Set<String> acted = new HashSet<>();
        if (lastActorId != null) {
            acted.add(lastActorId);
        }
        int normalizedRound = Math.max(1, roundNumber);
        if (store instanceof GameSessionTurnRuntimeAccess runtimeAccess) {
            List<TurnPersonalFrame> frames = runtimeAccess.personalTurnFrames();
            if (frames != null && !frames.isEmpty()) {
                for (TurnPersonalFrame frame : frames) {
                    String userId = frame.userId();
                    if (userId == null) {
                        continue;
                    }
                    TurnSnapshot snapshot = frame.currentSnapshot();
                    if (snapshot == null || snapshot.roundNumber() != normalizedRound) {
                        continue;
                    }
                    if (frame.outcomeStatus() != null) {
                        acted.add(userId);
                    }
                }
            }
        }
        for (String userId : orderUserIds) {
            if (!acted.contains(userId)) {
                return false;
            }
        }
        return true;
    }
}
