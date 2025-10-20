package teamnova.omok.glue.game.session.states.manage;

import java.util.Objects;

import teamnova.omok.glue.game.session.model.result.MoveResult;
import teamnova.omok.glue.game.session.model.result.ReadyResult;
import teamnova.omok.glue.game.session.model.result.TurnTimeoutResult;
import teamnova.omok.glue.game.session.model.runtime.TurnTransition;

/**
 * Stateless operations for managing turn-related buffers within {@link GameSessionStateContext}.
 */
public final class GameSessionTurnContextService {

    public void beginTurnCycle(GameSessionStateContext context, TurnCycleContext turnCycle) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(turnCycle, "turnCycle");
        if (context.turnRuntime().getActiveTurnCycle() != null) {
            throw new IllegalStateException("Turn cycle already in progress");
        }
        context.turnRuntime().setActiveTurnCycle(turnCycle);
        context.turnRuntime().clearPendingMoveResult();
    }

    public TurnCycleContext activeTurnCycle(GameSessionStateContext context) {
        Objects.requireNonNull(context, "context");
        return context.turnRuntime().getActiveTurnCycle();
    }

    public void clearTurnCycle(GameSessionStateContext context) {
        Objects.requireNonNull(context, "context");
        context.turnRuntime().clearActiveTurnCycle();
    }

    public void queueMoveResult(GameSessionStateContext context, MoveResult result) {
        Objects.requireNonNull(context, "context");
        context.turnRuntime().setPendingMoveResult(result);
    }

    public MoveResult consumeMoveResult(GameSessionStateContext context) {
        Objects.requireNonNull(context, "context");
        MoveResult result = context.turnRuntime().getPendingMoveResult();
        context.turnRuntime().clearPendingMoveResult();
        return result;
    }

    public void queueReadyResult(GameSessionStateContext context, ReadyResult result) {
        Objects.requireNonNull(context, "context");
        context.turnRuntime().setPendingReadyResult(result);
    }

    public ReadyResult consumeReadyResult(GameSessionStateContext context) {
        Objects.requireNonNull(context, "context");
        ReadyResult result = context.turnRuntime().getPendingReadyResult();
        context.turnRuntime().clearPendingReadyResult();
        return result;
    }

    public void queueTimeoutResult(GameSessionStateContext context, TurnTimeoutResult result) {
        Objects.requireNonNull(context, "context");
        context.turnRuntime().setPendingTimeoutResult(result);
    }

    public TurnTimeoutResult consumeTimeoutResult(GameSessionStateContext context) {
        Objects.requireNonNull(context, "context");
        TurnTimeoutResult result = context.turnRuntime().getPendingTimeoutResult();
        context.turnRuntime().clearPendingTimeoutResult();
        return result;
    }

    public void recordTurnTransition(GameSessionStateContext context,
                                     TurnTransition transition) {
        Objects.requireNonNull(context, "context");
        context.turnRuntime().setPendingTurnTransition(transition);
    }

    public TurnTransition peekTurnTransition(GameSessionStateContext context) {
        Objects.requireNonNull(context, "context");
        return context.turnRuntime().getPendingTurnTransition();
    }

    public TurnTransition consumeTurnTransition(GameSessionStateContext context) {
        Objects.requireNonNull(context, "context");
        TurnTransition transition = context.turnRuntime().getPendingTurnTransition();
        context.turnRuntime().clearPendingTurnTransition();
        return transition;
    }
}
