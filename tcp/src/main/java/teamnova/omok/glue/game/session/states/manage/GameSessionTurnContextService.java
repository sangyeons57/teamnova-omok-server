package teamnova.omok.glue.game.session.states.manage;

import java.util.Objects;

import teamnova.omok.glue.game.session.model.result.MoveResult;
import teamnova.omok.glue.game.session.model.result.ReadyResult;
import teamnova.omok.glue.game.session.model.result.TurnTimeoutResult;

/**
 * Stateless operations for managing turn-related buffers within {@link GameSessionStateContext}.
 */
public final class GameSessionTurnContextService {

    public void beginTurnCycle(GameSessionStateContext context, TurnCycleContext turnCycle) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(turnCycle, "turnCycle");
        if (context.getActiveTurnCycle() != null) {
            throw new IllegalStateException("Turn cycle already in progress");
        }
        context.setActiveTurnCycle(turnCycle);
        context.clearPendingMoveResult();
    }

    public TurnCycleContext activeTurnCycle(GameSessionStateContext context) {
        Objects.requireNonNull(context, "context");
        return context.getActiveTurnCycle();
    }

    public void clearTurnCycle(GameSessionStateContext context) {
        Objects.requireNonNull(context, "context");
        context.clearActiveTurnCycle();
    }

    public void queueMoveResult(GameSessionStateContext context, MoveResult result) {
        Objects.requireNonNull(context, "context");
        context.setPendingMoveResult(result);
    }

    public MoveResult consumeMoveResult(GameSessionStateContext context) {
        Objects.requireNonNull(context, "context");
        MoveResult result = context.getPendingMoveResult();
        context.clearPendingMoveResult();
        return result;
    }

    public void queueReadyResult(GameSessionStateContext context, ReadyResult result) {
        Objects.requireNonNull(context, "context");
        context.setPendingReadyResult(result);
    }

    public ReadyResult consumeReadyResult(GameSessionStateContext context) {
        Objects.requireNonNull(context, "context");
        ReadyResult result = context.getPendingReadyResult();
        context.clearPendingReadyResult();
        return result;
    }

    public void queueTimeoutResult(GameSessionStateContext context, TurnTimeoutResult result) {
        Objects.requireNonNull(context, "context");
        context.setPendingTimeoutResult(result);
    }

    public TurnTimeoutResult consumeTimeoutResult(GameSessionStateContext context) {
        Objects.requireNonNull(context, "context");
        TurnTimeoutResult result = context.getPendingTimeoutResult();
        context.clearPendingTimeoutResult();
        return result;
    }

    public void recordTurnTransition(GameSessionStateContext context,
                                     GameSessionStateContext.TurnTransition transition) {
        Objects.requireNonNull(context, "context");
        context.setPendingTurnTransition(transition);
    }

    public GameSessionStateContext.TurnTransition peekTurnTransition(GameSessionStateContext context) {
        Objects.requireNonNull(context, "context");
        return context.getPendingTurnTransition();
    }

    public GameSessionStateContext.TurnTransition consumeTurnTransition(GameSessionStateContext context) {
        Objects.requireNonNull(context, "context");
        GameSessionStateContext.TurnTransition transition = context.getPendingTurnTransition();
        context.clearPendingTurnTransition();
        return transition;
    }
}
