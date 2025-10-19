package teamnova.omok.glue.game.session.states.state;

import java.util.Objects;

import teamnova.omok.glue.game.session.interfaces.GameTurnService;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContext;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateType;
import teamnova.omok.glue.game.session.states.manage.TurnCycleContext;
import teamnova.omok.glue.game.session.model.result.MoveResult;
import teamnova.omok.modules.state_machine.interfaces.BaseState;
import teamnova.omok.modules.state_machine.interfaces.StateContext;
import teamnova.omok.modules.state_machine.models.StateName;
import teamnova.omok.modules.state_machine.models.StateStep;

/**
 * Advances the turn order when the game continues.
 */
public final class TurnFinalizingState implements BaseState {
    private final GameTurnService turnService;

    public TurnFinalizingState(GameTurnService turnService) {
        this.turnService = Objects.requireNonNull(turnService, "turnService");
    }
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
        GameTurnService.TurnSnapshot nextSnapshot = turnService
            .advanceSkippingDisconnected(
                context.session().getTurnStore(),
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
