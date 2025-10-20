package teamnova.omok.glue.game.session.model.runtime;

import teamnova.omok.glue.game.session.interfaces.GameTurnService;
import teamnova.omok.glue.game.session.services.RuleTurnStateView;

/**
 * Captures data produced when advancing a turn, including rule-facing views.
 */
public final class TurnTransition {
    private final GameTurnService.TurnSnapshot currentSnapshot;
    private final GameTurnService.TurnSnapshot nextSnapshot;
    private final RuleTurnStateView view;

    public TurnTransition(GameTurnService.TurnSnapshot currentSnapshot,
                          GameTurnService.TurnSnapshot nextSnapshot,
                          RuleTurnStateView view) {
        this.currentSnapshot = currentSnapshot;
        this.nextSnapshot = nextSnapshot;
        this.view = view;
    }

    public GameTurnService.TurnSnapshot currentSnapshot() {
        return currentSnapshot;
    }

    public GameTurnService.TurnSnapshot nextSnapshot() {
        return nextSnapshot;
    }

    public RuleTurnStateView view() {
        return view;
    }

    public boolean roundWrapped() {
        return nextSnapshot != null && nextSnapshot.wrapped();
    }
}
