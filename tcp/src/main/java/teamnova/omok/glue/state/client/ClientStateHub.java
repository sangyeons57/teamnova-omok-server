package teamnova.omok.glue.state.client;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import teamnova.omok.core.nio.ClientSession;
import teamnova.omok.glue.state.client.event.AuthenticatedClientEvent;
import teamnova.omok.glue.state.client.event.CancelMatchingClientEvent;
import teamnova.omok.glue.state.client.event.DisconnectClientEvent;
import teamnova.omok.glue.state.client.event.EnterGameClientEvent;
import teamnova.omok.glue.state.client.event.LeaveGameClientEvent;
import teamnova.omok.glue.state.client.event.ResetClientEvent;
import teamnova.omok.glue.state.client.event.StartMatchingClientEvent;
import teamnova.omok.glue.state.client.manage.ClientStateContext;
import teamnova.omok.glue.state.client.manage.ClientStateType;
import teamnova.omok.glue.state.client.state.AuthenticatedClientState;
import teamnova.omok.glue.state.client.state.ConnectedClientState;
import teamnova.omok.glue.state.client.state.DisconnectedClientState;
import teamnova.omok.glue.state.client.state.InGameClientState;
import teamnova.omok.glue.state.client.state.MatchingClientState;
import teamnova.omok.modules.state_machine.StateMachineGateway;
import teamnova.omok.modules.state_machine.StateMachineGateway.Handle;
import teamnova.omok.modules.state_machine.interfaces.BaseEvent;
import teamnova.omok.modules.state_machine.interfaces.BaseState;
import teamnova.omok.modules.state_machine.models.StateName;
import teamnova.omok.glue.state.game.GameStateHub;

/**
 * Coordinates client-level transitions using the shared state machine module.
 */
public final class ClientStateHub {
    private static final Map<StateName, ClientStateType> STATE_NAME_LOOKUP =
        Arrays.stream(ClientStateType.values())
            .collect(Collectors.toUnmodifiableMap(ClientStateType::toStateName, type -> type));

    private final Handle stateMachine;
    private final ClientStateContext context;
    private ClientStateType currentType;

    public ClientStateHub(ClientSession session) {
        Objects.requireNonNull(session, "session");
        this.context = new ClientStateContext(session);
        this.stateMachine = StateMachineGateway.open();
        this.stateMachine.onTransition(this::handleTransition);

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
        ClientStateType resolved = STATE_NAME_LOOKUP.get(stateName);
        if (resolved == null) {
            throw new IllegalStateException("Unrecognised client state: " + stateName.name());
        }
        currentType = resolved;
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
        submit(event, null);
    }

    public void submit(BaseEvent event, Consumer<ClientStateContext> callback) {
        Objects.requireNonNull(event, "event");
        stateMachine.submit(event, ctx -> {
            if (callback != null) {
                callback.accept((ClientStateContext) ctx);
            }
        });
    }

    public void process(long now) {
        stateMachine.process(context, now);
    }
}
