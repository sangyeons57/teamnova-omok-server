package teamnova.omok.glue.client.session.states.state;

import teamnova.omok.glue.client.session.states.event.DisconnectClientEvent;
import teamnova.omok.glue.client.session.states.event.LeaveGameClientEvent;
import teamnova.omok.glue.client.session.states.event.ResetClientEvent;
import teamnova.omok.glue.client.session.states.manage.ClientStateContext;
import teamnova.omok.glue.client.session.states.manage.ClientStateType;
import teamnova.omok.modules.state_machine.interfaces.BaseEvent;
import teamnova.omok.modules.state_machine.interfaces.BaseState;
import teamnova.omok.modules.state_machine.interfaces.StateContext;
import teamnova.omok.modules.state_machine.models.StateName;
import teamnova.omok.modules.state_machine.models.StateStep;

/**
 * State while the client participates in an in-progress game.
 */
public final class InGameClientState implements BaseState {
    @Override
    public StateName name() {
        return ClientStateType.IN_GAME.toStateName();
    }

    @Override
    public <I extends StateContext> StateStep onEvent(I context, BaseEvent event) {
        if (event instanceof LeaveGameClientEvent) {
            return StateStep.transition(ClientStateType.AUTHENTICATED.toStateName());
        }
        if (event instanceof DisconnectClientEvent) {
            return StateStep.transition(ClientStateType.DISCONNECTED.toStateName());
        }
        if (event instanceof ResetClientEvent) {
            return StateStep.transition(ClientStateType.CONNECTED.toStateName());
        }
        return StateStep.stay();
    }

    @Override
    public <I extends StateContext> void onExit(I context) {
        ClientStateContext clientContext = (ClientStateContext) context;
        clientContext.clearGame();
    }
}
