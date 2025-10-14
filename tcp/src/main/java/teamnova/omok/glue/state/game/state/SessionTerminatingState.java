package teamnova.omok.glue.state.game.state;

import teamnova.omok.glue.state.game.manage.GameSessionStateContext;
import teamnova.omok.glue.state.game.manage.GameSessionStateType;
import teamnova.omok.modules.state_machine.interfaces.BaseState;
import teamnova.omok.modules.state_machine.interfaces.StateContext;
import teamnova.omok.modules.state_machine.models.StateName;
import teamnova.omok.modules.state_machine.models.StateStep;

/**
 * Ensures decision timers are cleared before the session is fully terminated.
 */
public final class SessionTerminatingState implements BaseState {
    @Override
    public StateName name() {
        return GameSessionStateType.SESSION_TERMINATING.toStateName();
    }

    @Override
    public <I extends StateContext> StateStep onEnter(I context) {
        return onEnterInternal((GameSessionStateContext) context);
    }

    private StateStep onEnterInternal(GameSessionStateContext context) {
        context.clearPostGameDecisionDeadline();
        return StateStep.transition(GameSessionStateType.COMPLETED.toStateName());
    }
}
