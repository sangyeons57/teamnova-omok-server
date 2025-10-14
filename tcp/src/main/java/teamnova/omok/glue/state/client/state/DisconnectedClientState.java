package teamnova.omok.glue.state.client.state;

import teamnova.omok.glue.state.client.event.DisconnectClientEvent;
import teamnova.omok.glue.state.client.event.ResetClientEvent;
import teamnova.omok.glue.state.client.manage.ClientStateContext;
import teamnova.omok.glue.state.client.manage.ClientStateType;
import teamnova.omok.modules.state_machine.interfaces.BaseEvent;
import teamnova.omok.modules.state_machine.interfaces.BaseState;
import teamnova.omok.modules.state_machine.interfaces.StateContext;
import teamnova.omok.modules.state_machine.models.StateName;
import teamnova.omok.modules.state_machine.models.StateStep;

/**
 * Terminal state once the client session has been closed.
 */
public final class DisconnectedClientState implements BaseState {
    @Override
    public StateName name() {
        return ClientStateType.DISCONNECTED.toStateName();
    }

    @Override
    public <I extends StateContext> StateStep onEnter(I context) {
        ClientStateContext clientContext = (ClientStateContext) context;
        clientContext.clearGame();
        return StateStep.stay();
    }

    @Override
    public <I extends StateContext> StateStep onEvent(I context, BaseEvent event) {
        if (event instanceof ResetClientEvent) {
            ClientStateContext clientContext = (ClientStateContext) context;
            clientContext.clearGame();
            return StateStep.transition(ClientStateType.CONNECTED.toStateName());
        }
        if (event instanceof DisconnectClientEvent) {
            return StateStep.stay();
        }
        return StateStep.stay();
    }
}

