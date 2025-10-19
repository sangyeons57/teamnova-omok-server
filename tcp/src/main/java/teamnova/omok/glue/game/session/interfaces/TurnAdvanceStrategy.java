package teamnova.omok.glue.game.session.interfaces;

import java.util.Optional;
import java.util.Set;

import teamnova.omok.glue.game.session.model.vo.TurnOrder;

/**
 * Defines how the next active participant is chosen for a turn.
 */
public interface TurnAdvanceStrategy {

    Optional<Result> next(TurnOrder order, int currentIndex, Set<String> skip);

    record Result(int nextIndex, boolean wrapped) { }
}

