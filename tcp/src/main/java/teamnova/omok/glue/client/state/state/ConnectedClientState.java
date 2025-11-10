package teamnova.omok.glue.client.state.state;

import teamnova.omok.glue.client.state.event.AuthenticatedClientEvent;
import teamnova.omok.glue.client.state.event.DisconnectClientEvent;
import teamnova.omok.glue.client.state.event.ResetClientEvent;
import teamnova.omok.glue.client.state.manage.ClientStateContext;
import teamnova.omok.glue.client.state.manage.ClientStateType;
import teamnova.omok.modules.state_machine.interfaces.BaseEvent;
import teamnova.omok.modules.state_machine.interfaces.BaseState;
import teamnova.omok.modules.state_machine.interfaces.StateContext;
import teamnova.omok.modules.state_machine.models.StateName;
import teamnova.omok.modules.state_machine.models.StateStep;

/**
 * Initial connection state before authentication.
 */
public final class ConnectedClientState implements BaseState {
    @Override
    public StateName name() {
        return ClientStateType.CONNECTED.toStateName();
    }

    @Override
    public <I extends StateContext> StateStep onEvent(I context, BaseEvent event) {
        ClientStateContext clientContext = (ClientStateContext) context;
        if (event instanceof AuthenticatedClientEvent) {
            return StateStep.transition(ClientStateType.AUTHENTICATED.toStateName());
        }
        if (event instanceof DisconnectClientEvent) {
            clientContext.clearGame();
            return StateStep.transition(ClientStateType.DISCONNECTED.toStateName());
        }
        if (event instanceof ResetClientEvent) {
            clientContext.clearGame();
            return StateStep.stay();
        }
        return StateStep.stay();
    }
}
