package teamnova.omok.glue.state.game.state;

import teamnova.omok.glue.service.TurnService;
import teamnova.omok.glue.service.dto.MoveResult;
import teamnova.omok.glue.state.game.manage.GameSessionStateContext;
import teamnova.omok.glue.state.game.manage.GameSessionStateType;
import teamnova.omok.glue.state.game.manage.TurnCycleContext;
import teamnova.omok.modules.state_machine.interfaces.BaseState;
import teamnova.omok.modules.state_machine.interfaces.StateContext;
import teamnova.omok.modules.state_machine.models.StateName;
import teamnova.omok.modules.state_machine.models.StateStep;

/**
 * Advances the turn order when the game continues.
 */
public final class TurnFinalizingState implements BaseState {
    @Override
    public StateName name() {
        return GameSessionStateType.TURN_FINALIZING.toStateName();
    }

    @Override
    public <I extends StateContext> StateStep onEnter(I context) {
        return onEnterInternal((GameSessionStateContext) context);
    }

    private StateStep onEnterInternal(GameSessionStateContext context) {
        TurnCycleContext cycle = context.activeTurnCycle();
        if (cycle == null) {
            return StateStep.transition(GameSessionStateType.TURN_WAITING.toStateName());
        }
        TurnService.TurnSnapshot nextSnapshot = context.turnService()
            .advanceSkippingDisconnected(
                context.session().getTurnStore(),
                context.session().getUserIds(),
                context.session().disconnectedUsersView(),
                cycle.now()
            );
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
        return StateStep.transition(GameSessionStateType.TURN_WAITING.toStateName());
    }
}
