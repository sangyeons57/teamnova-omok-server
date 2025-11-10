package teamnova.omok.glue.client.state.state;

import teamnova.omok.glue.client.state.event.CancelMatchingClientEvent;
import teamnova.omok.glue.client.state.event.DisconnectClientEvent;
import teamnova.omok.glue.client.state.event.FinishReconnectingClientEvent;
import teamnova.omok.glue.client.state.event.ResetClientEvent;
import teamnova.omok.glue.client.state.manage.ClientStateContext;
import teamnova.omok.glue.client.state.manage.ClientStateType;
import teamnova.omok.modules.state_machine.interfaces.BaseEvent;
import teamnova.omok.modules.state_machine.interfaces.BaseState;
import teamnova.omok.modules.state_machine.interfaces.StateContext;
import teamnova.omok.modules.state_machine.models.StateName;
import teamnova.omok.modules.state_machine.models.StateStep;

/**
 * Transitional state while a client is attempting to reconnect into an active session.
 */
public final class ReconnectingClientState implements BaseState {
    @Override
    public StateName name() {
        return ClientStateType.RECONNECTING.toStateName();
    }

    @Override
    public <I extends StateContext> StateStep onEvent(I context, BaseEvent event) {
        if (event instanceof FinishReconnectingClientEvent finish) {
            if (finish.success()) {
                return StateStep.transition(ClientStateType.IN_GAME.toStateName());
            }
            return StateStep.transition(ClientStateType.AUTHENTICATED.toStateName());
        }
        if (event instanceof DisconnectClientEvent) {
            ClientStateContext clientContext = (ClientStateContext) context;
            clientContext.clearGame();
            return StateStep.transition(ClientStateType.DISCONNECTED.toStateName());
        }
        if (event instanceof ResetClientEvent) {
            ClientStateContext clientContext = (ClientStateContext) context;
            clientContext.clearGame();
            return StateStep.transition(ClientStateType.CONNECTED.toStateName());
        }
        if (event instanceof CancelMatchingClientEvent) {
            return StateStep.transition(ClientStateType.AUTHENTICATED.toStateName());
        }
        return StateStep.stay();
    }
}
