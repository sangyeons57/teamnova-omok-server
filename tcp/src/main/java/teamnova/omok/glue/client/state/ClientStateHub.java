package teamnova.omok.glue.client.state;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import teamnova.omok.glue.client.session.interfaces.ClientSessionHandle;
import teamnova.omok.glue.client.session.interfaces.ClientSessionStateListener;
import teamnova.omok.glue.client.session.services.ClientSessionStore;
import teamnova.omok.glue.client.state.manage.ClientStateContext;
import teamnova.omok.glue.client.state.manage.ClientStateType;
import teamnova.omok.glue.client.state.state.AuthenticatedClientState;
import teamnova.omok.glue.client.state.state.ConnectedClientState;
import teamnova.omok.glue.client.state.state.DisconnectedClientState;
import teamnova.omok.glue.client.state.state.InGameClientState;
import teamnova.omok.glue.client.state.state.MatchingClientState;
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
    private final List<ClientSessionStateListener> stateListeners = new CopyOnWriteArrayList<>();
    private final Object processLock = new Object();

    public ClientStateHub(ClientSessionHandle session,
                          ClientSessionStore store) {
        Objects.requireNonNull(session, "session");
        this.context = new ClientStateContext(session,
            Objects.requireNonNull(store, "store"));
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
        ClientStateType previous = currentType;
        currentType = ClientStateType.fromStateName(stateName);
        notifyStateListeners(previous, currentType);
    }

    private void notifyStateListeners(ClientStateType previous, ClientStateType current) {
        if (stateListeners.isEmpty()) {
            return;
        }
        for (ClientSessionStateListener listener : stateListeners) {
            try {
                listener.onStateChanged(context.clientSession(), previous, current);
            } catch (RuntimeException ex) {
                System.err.println("[CLIENT][state] listener failed: " + ex.getMessage());
            }
        }
    }

    public ClientStateType currentType() {
        return currentType;
    }

    public ClientStateContext context() {
        return context;
    }

    public void submit(BaseEvent event) {
        Objects.requireNonNull(event, "event");
        stateMachine.submit(event);
    }

    public void addStateListener(ClientSessionStateListener listener) {
        if (listener != null) {
            stateListeners.add(listener);
        }
    }

    public void process(long now) {
        synchronized (processLock) {
            stateMachine.process(context, now);
        }
    }

    public void drainPending() {
        process(System.currentTimeMillis());
    }
}
