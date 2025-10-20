package teamnova.omok.glue.game.session.model.store;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionTurnRuntimeAccess;
import teamnova.omok.glue.game.session.model.result.MoveResult;
import teamnova.omok.glue.game.session.model.result.ReadyResult;
import teamnova.omok.glue.game.session.model.result.TurnTimeoutResult;
import teamnova.omok.glue.game.session.model.runtime.TurnTransition;
import teamnova.omok.glue.game.session.states.manage.TurnCycleContext;

/**
 * Holds transient turn-processing buffers for a {@link teamnova.omok.glue.game.session.model.GameSession}.
 */
public final class TurnRuntimeStore implements GameSessionTurnRuntimeAccess {
    private TurnCycleContext activeTurnCycle;
    private MoveResult pendingMoveResult;
    private ReadyResult pendingReadyResult;
    private TurnTimeoutResult pendingTimeoutResult;
    private TurnTransition pendingTurnTransition;

    @Override
    public TurnCycleContext getActiveTurnCycle() {
        return activeTurnCycle;
    }

    @Override
    public void setActiveTurnCycle(TurnCycleContext context) {
        this.activeTurnCycle = context;
    }

    @Override
    public void clearActiveTurnCycle() {
        this.activeTurnCycle = null;
    }

    @Override
    public MoveResult getPendingMoveResult() {
        return pendingMoveResult;
    }

    @Override
    public void setPendingMoveResult(MoveResult result) {
        this.pendingMoveResult = result;
    }

    @Override
    public void clearPendingMoveResult() {
        this.pendingMoveResult = null;
    }

    @Override
    public ReadyResult getPendingReadyResult() {
        return pendingReadyResult;
    }

    @Override
    public void setPendingReadyResult(ReadyResult result) {
        this.pendingReadyResult = result;
    }

    @Override
    public void clearPendingReadyResult() {
        this.pendingReadyResult = null;
    }

    @Override
    public TurnTimeoutResult getPendingTimeoutResult() {
        return pendingTimeoutResult;
    }

    @Override
    public void setPendingTimeoutResult(TurnTimeoutResult result) {
        this.pendingTimeoutResult = result;
    }

    @Override
    public void clearPendingTimeoutResult() {
        this.pendingTimeoutResult = null;
    }

    @Override
    public TurnTransition getPendingTurnTransition() {
        return pendingTurnTransition;
    }

    @Override
    public void setPendingTurnTransition(TurnTransition transition) {
        this.pendingTurnTransition = transition;
    }

    @Override
    public void clearPendingTurnTransition() {
        this.pendingTurnTransition = null;
    }
}
