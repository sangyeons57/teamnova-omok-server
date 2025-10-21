package teamnova.omok.glue.game.session.services;

import java.util.Objects;

import teamnova.omok.glue.game.session.interfaces.GameTurnService;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContext;

/**
 * Aggregates immutable turn data snapshots for rule evaluation.
 */
public final class RuleTurnStateView {
    private final GameTurnService.TurnSnapshot currentSnapshot;
    private final GameTurnService.TurnSnapshot nextSnapshot;

    private RuleTurnStateView(GameTurnService.TurnSnapshot currentSnapshot,
                              GameTurnService.TurnSnapshot nextSnapshot) {
        this.currentSnapshot = currentSnapshot;
        this.nextSnapshot = nextSnapshot;
    }

    public static RuleTurnStateView fromAdvance(GameTurnService.TurnSnapshot currentSnapshot,
                                                GameTurnService.TurnSnapshot nextSnapshot) {
        return new RuleTurnStateView(currentSnapshot, nextSnapshot);
    }

    public static RuleTurnStateView fromTurnStart(GameTurnService.TurnSnapshot snapshot) {
        Objects.requireNonNull(snapshot, "snapshot");
        return new RuleTurnStateView(null, snapshot);
    }

    public static RuleTurnStateView capture(GameSessionStateContext context,
                                            GameTurnService turnService) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(turnService, "turnService");
        GameTurnService.TurnSnapshot snapshot = turnService.snapshot(context.turns());
        return new RuleTurnStateView(snapshot, null);
    }

    public GameTurnService.TurnSnapshot currentSnapshot() {
        return currentSnapshot;
    }

    public GameTurnService.TurnSnapshot nextSnapshot() {
        return nextSnapshot;
    }

    public GameTurnService.TurnSnapshot resolvedSnapshot() {
        return nextSnapshot != null ? nextSnapshot : currentSnapshot;
    }
}
