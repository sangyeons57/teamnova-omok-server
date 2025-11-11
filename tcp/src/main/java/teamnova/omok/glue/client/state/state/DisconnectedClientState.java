package teamnova.omok.glue.client.state.state;

import java.util.concurrent.TimeUnit;

import teamnova.omok.glue.client.state.event.DisconnectClientEvent;
import teamnova.omok.glue.client.state.event.ResetClientEvent;
import teamnova.omok.glue.client.state.event.TerminateClientEvent;
import teamnova.omok.glue.client.state.manage.ClientStateContext;
import teamnova.omok.glue.client.state.manage.ClientStateType;
import teamnova.omok.modules.state_machine.interfaces.BaseEvent;
import teamnova.omok.modules.state_machine.interfaces.BaseState;
import teamnova.omok.modules.state_machine.interfaces.StateContext;
import teamnova.omok.modules.state_machine.models.StateName;
import teamnova.omok.modules.state_machine.models.StateStep;

/**
 * Handles the transport-disconnected period before a session is finally terminated.
 */
public final class DisconnectedClientState implements BaseState {
    private static final long DISCONNECT_GRACE_MILLIS = TimeUnit.MINUTES.toMillis(2);

    @Override
    public StateName name() {
        return ClientStateType.DISCONNECTED.toStateName();
    }

    @Override
    public <I extends StateContext> StateStep onEnter(I context) {
        ClientStateContext clientContext = (ClientStateContext) context;
        clientContext.markDisconnectedNow();
        clientContext.notifyGameSessionDisconnected();
        return StateStep.stay();
    }

    @Override
    public <I extends StateContext> StateStep onUpdate(I context, long now) {
        ClientStateContext clientContext = (ClientStateContext) context;
        if (clientContext.hasExceededDisconnectGrace(now, DISCONNECT_GRACE_MILLIS)) {
            return StateStep.transition(ClientStateType.TERMINATED.toStateName());
        }
        if (clientContext.gameStateManager() == null) {
            return StateStep.transition(ClientStateType.TERMINATED.toStateName());
        }
        return StateStep.stay();
    }

    @Override
    public <I extends StateContext> StateStep onEvent(I context, BaseEvent event) {
        ClientStateContext clientContext = (ClientStateContext) context;
        if (event instanceof ResetClientEvent) {
            clientContext.clearGame();
            clientContext.clearDisconnectionTimer();
            return StateStep.transition(ClientStateType.CONNECTED.toStateName());
        }
        if (event instanceof DisconnectClientEvent) {
            return StateStep.stay();
        }
        if (event instanceof TerminateClientEvent) {
            return StateStep.transition(ClientStateType.TERMINATED.toStateName());
        }
        return StateStep.stay();
    }
}
