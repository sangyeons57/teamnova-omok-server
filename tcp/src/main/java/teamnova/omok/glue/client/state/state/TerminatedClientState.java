package teamnova.omok.glue.client.state.state;

import teamnova.omok.glue.client.state.manage.ClientStateContext;
import teamnova.omok.glue.client.state.manage.ClientStateType;
import teamnova.omok.modules.state_machine.interfaces.BaseState;
import teamnova.omok.modules.state_machine.interfaces.StateContext;
import teamnova.omok.modules.state_machine.models.StateName;
import teamnova.omok.modules.state_machine.models.StateStep;

// 이상태가 필요한 이유 해당 세션을 제거하고 작동을 완전히 멈추기 위한 상태
public class TerminatedClientState implements BaseState {
    @Override
    public StateName name() {
        return ClientStateType.TERMINATED.toStateName();
    }

    public <I extends StateContext> StateStep onEnter(I context) {
        return internalOnEnter((ClientStateContext)context);
    }

    private StateStep internalOnEnter(ClientStateContext context) {
        context.terminateSession();
        return StateStep.stay();
    }
}
