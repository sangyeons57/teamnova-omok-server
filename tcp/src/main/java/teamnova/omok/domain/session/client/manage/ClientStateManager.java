package teamnova.omok.domain.session.client.manage;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import teamnova.omok.nio.ClientSession;
import teamnova.omok.domain.session.client.contract.ClientState;
import teamnova.omok.domain.session.client.event.AuthenticatedClientEvent;
import teamnova.omok.domain.session.client.event.CancelMatchingClientEvent;
import teamnova.omok.domain.session.client.event.ClientEvent;
import teamnova.omok.domain.session.client.event.ClientEventRegistry;
import teamnova.omok.domain.session.client.event.ClientEventRegistry.HandlerEntry;
import teamnova.omok.domain.session.client.event.ClientEventType;
import teamnova.omok.domain.session.client.event.DisconnectClientEvent;
import teamnova.omok.domain.session.client.event.EnterGameClientEvent;
import teamnova.omok.domain.session.client.event.LeaveGameClientEvent;
import teamnova.omok.domain.session.client.event.StartMatchingClientEvent;
import teamnova.omok.domain.session.client.state.AuthenticatedClientState;
import teamnova.omok.domain.session.client.state.ConnectedClientState;
import teamnova.omok.domain.session.client.state.DisconnectedClientState;
import teamnova.omok.domain.session.client.state.InGameClientState;
import teamnova.omok.domain.session.client.state.MatchingClientState;

/**
 * Coordinates client-level state transitions and routes incoming client events.
 */
public final class ClientStateManager {
    private final ClientStateContext context;
    private final Map<ClientStateType, StateRegistration> registrations =
        new EnumMap<>(ClientStateType.class);
    private final Queue<ClientEvent> eventQueue = new ConcurrentLinkedQueue<>();
    private StateRegistration currentRegistration;

    public ClientStateManager(ClientSession session) {
        Objects.requireNonNull(session, "session");
        this.context = new ClientStateContext(session);
        registerState(ClientStateType.CONNECTED, new ConnectedClientState());
        registerState(ClientStateType.AUTHENTICATED, new AuthenticatedClientState());
        registerState(ClientStateType.MATCHING, new MatchingClientState());
        registerState(ClientStateType.IN_GAME, new InGameClientState());
        registerState(ClientStateType.DISCONNECTED, new DisconnectedClientState());
        this.currentRegistration = registrations.get(ClientStateType.CONNECTED);
        if (currentRegistration == null) {
            throw new IllegalStateException("Connected state not registered");
        }
        applyTransition(currentRegistration.state.onEnter(context));
    }

    private void registerState(ClientStateType type, ClientState state) {
        if (state.type() != type) {
            throw new IllegalArgumentException("State type mismatch: expected " + type + " but was " + state.type());
        }
        ClientEventRegistry registry = new ClientEventRegistry();
        state.registerHandlers(registry);
        registrations.put(type, new StateRegistration(state, registry.handlers()));
    }

    public ClientStateType currentType() {
        return currentRegistration.state.type();
    }

    public ClientStateContext context() {
        return context;
    }

    public void markAuthenticated() {
        submit(new AuthenticatedClientEvent());
    }

    public void startMatching() {
        submit(new StartMatchingClientEvent());
    }

    public void cancelMatching() {
        submit(new CancelMatchingClientEvent());
    }

    public void enterGame(GameSessionStateManager manager) {
        submit(new EnterGameClientEvent(manager));
    }

    public void leaveGame() {
        submit(new LeaveGameClientEvent());
    }

    public void disconnect() {
        submit(new DisconnectClientEvent());
    }

    public void submit(ClientEvent event) {
        Objects.requireNonNull(event, "event");
        eventQueue.add(event);
        drainEvents();
    }

    public void update(long now) {
        ClientStateStep step = currentRegistration.state.onUpdate(context, now);
        applyTransition(step);
    }

    public void resetToConnected() {
        transitionDirect(ClientStateType.CONNECTED);
    }

    private void drainEvents() {
        ClientEvent event;
        while ((event = eventQueue.poll()) != null) {
            handleEvent(event);
        }
    }

    private void handleEvent(ClientEvent event) {
        HandlerEntry entry = currentRegistration.handlers.get(event.type());
        ClientStateStep step = (entry != null)
            ? entry.invoke(context, event)
            : ClientStateStep.stay();
        applyTransition(step);
    }

    private void applyTransition(ClientStateStep step) {
        if (step != null && step.hasTransition()) {
            transitionDirect(step.nextState());
        }
    }

    private void transitionDirect(ClientStateType targetType) {
        StateRegistration next = registrations.get(targetType);
        if (next == null) {
            throw new IllegalStateException("No client state registered for type " + targetType);
        }
        if (next == currentRegistration) {
            return;
        }
        currentRegistration.state.onExit(context);
        currentRegistration = next;
        ClientStateStep entryStep = currentRegistration.state.onEnter(context);
        applyTransition(entryStep);
    }

    private record StateRegistration(ClientState state,
                                     Map<ClientEventType, HandlerEntry> handlers) { }
}
