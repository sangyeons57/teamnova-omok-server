package teamnova.omok.domain.session.game.entity.state.state;

import teamnova.omok.service.dto.MoveResult;
import teamnova.omok.domain.session.game.entity.turn.TurnSnapshot;
import teamnova.omok.domain.session.game.entity.state.contract.GameSessionState;
import teamnova.omok.domain.session.game.entity.state.manage.GameSessionStateContext;
import teamnova.omok.domain.session.game.entity.state.manage.GameSessionStateStep;
import teamnova.omok.domain.session.game.entity.state.manage.GameSessionStateType;
import teamnova.omok.domain.session.game.entity.state.manage.TurnCycleContext;

/**
 * Advances the turn order when the game continues.
 */
public final class TurnFinalizingState implements GameSessionState {
    @Override
    public GameSessionStateType type() {
        return GameSessionStateType.TURN_FINALIZING;
    }

    @Override
    public GameSessionStateStep onEnter(GameSessionStateContext context) {
        TurnCycleContext cycle = context.activeTurnCycle();
        if (cycle == null) {
            return GameSessionStateStep.transition(GameSessionStateType.TURN_WAITING);
        }
        TurnSnapshot nextSnapshot = context.session().advanceSkippingDisconnected(cycle.now());
        cycle.snapshots().next(nextSnapshot);
        context.pendingMoveResult(MoveResult.success(
            cycle.session(),
            cycle.stone(),
            nextSnapshot,
            cycle.userId(),
            cycle.x(),
            cycle.y()
        ));
        context.clearTurnCycle();
        return GameSessionStateStep.transition(GameSessionStateType.TURN_WAITING);
    }
}
