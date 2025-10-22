package teamnova.omok.glue.client.session.states;

import java.util.Objects;

import teamnova.omok.glue.client.session.interfaces.ClientSessionHandle;
import teamnova.omok.glue.client.session.states.event.AuthenticatedClientEvent;
import teamnova.omok.glue.client.session.states.event.DisconnectClientEvent;
import teamnova.omok.glue.client.session.states.event.ResetClientEvent;
import teamnova.omok.glue.client.session.states.manage.ClientStateContext;
import teamnova.omok.glue.client.session.states.manage.ClientStateType;
import teamnova.omok.glue.client.session.states.state.AuthenticatedClientState;
import teamnova.omok.glue.client.session.states.state.ConnectedClientState;
import teamnova.omok.glue.client.session.states.state.DisconnectedClientState;
import teamnova.omok.glue.client.session.states.state.InGameClientState;
import teamnova.omok.glue.client.session.states.state.MatchingClientState;
import teamnova.omok.modules.state_machine.StateMachineGateway;
import teamnova.omok.modules.state_machine.StateMachineGateway.Handle;
import teamnova.omok.modules.state_machine.interfaces.BaseEvent;
import teamnova.omok.modules.state_machine.interfaces.BaseState;
import teamnova.omok.modules.state_machine.interfaces.StateSignalListener;
import teamnova.omok.modules.state_machine.models.LifecycleEventKind;
import teamnova.omok.modules.state_machine.models.StateName;

/**
 * Coordinates client-level transitions using the shared state machine module.
 */
public final class ClientStateHub {

    private final Handle stateMachine;
    private final ClientStateContext context;
    private ClientStateType currentType;

    public ClientStateHub(ClientSessionHandle session) {
        Objects.requireNonNull(session, "session");
        this.context = new ClientStateContext(session);
        this.stateMachine = StateMachineGateway.open();
        this.stateMachine.addStateSignalListener(new StateSignalListener() {
            private StateName pendingFrom;
            @Override
            public java.util.Set<LifecycleEventKind> events() {
                return java.util.Set.of(LifecycleEventKind.ON_TRANSITION, LifecycleEventKind.ON_START);
            }
            @Override
            public void onSignal(StateName state, LifecycleEventKind kind) {
                if (kind == LifecycleEventKind.ON_TRANSITION) {
                    // Remember the state we are leaving (from)
                    pendingFrom = state;
                } else if (kind == LifecycleEventKind.ON_START) {
                    // ON_START delivers the target state (to). Use it to update currentType.
                    // This pairs with the prior ON_TRANSITION for completeness.
                    handleTransition(state);
                    pendingFrom = null;
                }
            }
        });

        registerState(new ConnectedClientState());
        registerState(new AuthenticatedClientState());
        registerState(new MatchingClientState());
        registerState(new InGameClientState());
        registerState(new DisconnectedClientState());

        this.stateMachine.start(ClientStateType.CONNECTED.toStateName(), context);
    }

    private void registerState(BaseState state) {
        this.stateMachine.register(state);
    }

    private void handleTransition(StateName stateName) {
        currentType = ClientStateType.fromStateName(stateName);
        context.clientSession().model().updateState(currentType);
    }

    public ClientStateType currentType() {
        return currentType;
    }

    public ClientStateContext context() {
        return context;
    }

    public void markAuthenticated() {
        submit(new AuthenticatedClientEvent());
    }

    public void disconnect() {
        submit(new DisconnectClientEvent());
    }

    public void resetToConnected() {
        submit(new ResetClientEvent());
    }

    public void submit(BaseEvent event) {
        Objects.requireNonNull(event, "event");
        stateMachine.submit(event);
    }

    public void process(long now) {
        stateMachine.process(context, now);
    }
}
