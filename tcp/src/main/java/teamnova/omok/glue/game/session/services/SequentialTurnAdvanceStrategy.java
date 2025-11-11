package teamnova.omok.glue.game.session.services;

import java.util.Optional;

import teamnova.omok.glue.game.session.interfaces.TurnAdvanceStrategy;
import teamnova.omok.glue.game.session.model.vo.TurnOrder;

/**
 * Default turn advancement strategy: iterate participants sequentially and wrap to the first player.
 */
public final class SequentialTurnAdvanceStrategy implements TurnAdvanceStrategy {

    @Override
    public Optional<Result> next(TurnOrder order, int currentIndex) {
        if (order == null || order.size() == 0) {
            return Optional.empty();
        }
        int total = order.size();
        int cursor = currentIndex < 0 ? 0 : (currentIndex + 1) % total;
        boolean wrapped = currentIndex >= 0 && cursor <= currentIndex;
        return Optional.of(new Result(cursor, wrapped));
    }
}
