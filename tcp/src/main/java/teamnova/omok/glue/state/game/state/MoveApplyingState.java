package teamnova.omok.glue.state.game.state;

import teamnova.omok.glue.state.game.manage.GameSessionStateContext;
import teamnova.omok.glue.state.game.manage.GameSessionStateType;
import teamnova.omok.glue.state.game.manage.TurnCycleContext;
import teamnova.omok.modules.state_machine.interfaces.BaseState;
import teamnova.omok.modules.state_machine.interfaces.StateContext;
import teamnova.omok.modules.state_machine.models.StateName;
import teamnova.omok.modules.state_machine.models.StateStep;

/**
 * Writes the validated stone onto the board.
 */
public final class MoveApplyingState implements BaseState {
    @Override
    public StateName name() {
        return GameSessionStateType.MOVE_APPLYING.toStateName();
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
        context.boardService().setStone(
            context.session().getBoardStore(),
            cycle.x(),
            cycle.y(),
            cycle.stone()
        );
        return StateStep.transition(GameSessionStateType.OUTCOME_EVALUATING.toStateName());
    }
}
