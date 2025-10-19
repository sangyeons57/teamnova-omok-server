package teamnova.omok.glue.game.session.services;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import teamnova.omok.glue.game.session.interfaces.TurnAdvanceStrategy;
import teamnova.omok.glue.game.session.model.vo.TurnOrder;

/**
 * Default turn advancement strategy: iterate participants sequentially,
 * skipping disconnected players and wrapping to the first player.
 */
public final class SequentialTurnAdvanceStrategy implements TurnAdvanceStrategy {

    @Override
    public Optional<Result> next(TurnOrder order, int currentIndex, Set<String> skip) {
        if (order == null || order.size() == 0) {
            return Optional.empty();
        }
        Set<String> skipSet = (skip == null) ? Collections.emptySet() : skip;
        int total = order.size();
        int cursor = Math.max(-1, currentIndex);
        int checked = 0;
        while (checked < total) {
            cursor = (cursor + 1) % total;
            String candidate = order.userIdAt(cursor);
            if (!skipSet.contains(candidate)) {
                boolean wrapped = currentIndex >= 0 && cursor <= currentIndex;
                return Optional.of(new Result(cursor, wrapped));
            }
            checked++;
        }
        return Optional.empty();
    }
}

