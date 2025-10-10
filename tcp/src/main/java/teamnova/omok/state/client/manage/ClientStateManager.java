package teamnova.omok.state.client.manage;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

import teamnova.omok.nio.ClientSession;
import teamnova.omok.state.client.*;
import teamnova.omok.state.game.manage.GameSessionStateManager;

/**
 * Coordinates connection-level state transitions and optionally nests a game state manager
 * when the client participates in a running game.
 */
public class ClientStateManager {
    private final ClientStateContext context;
    private final Map<ClientStateType, ClientState> states = new EnumMap<>(ClientStateType.class);
    private ClientState currentState;

    public ClientStateManager(ClientSession session) {
        Objects.requireNonNull(session, "session");
        this.context = new ClientStateContext(session);
        states.put(ClientStateType.CONNECTED, new ConnectedClientState());
        states.put(ClientStateType.AUTHENTICATED, new AuthenticatedClientState());
        states.put(ClientStateType.MATCHING, new MatchingClientState());
        states.put(ClientStateType.IN_GAME, new InGameClientState());
        states.put(ClientStateType.DISCONNECTED, new DisconnectedClientState());
        this.currentState = states.get(ClientStateType.CONNECTED);
        this.currentState.onEnter(context);
    }

    public ClientStateType currentType() {
        return currentState.type();
    }

    public GameSessionStateManager currentGame() {
        return context.gameStateManager();
    }

    public void markAuthenticated() {
        applyTransition(currentState.handleAuthenticated(context));
    }

    public void startMatching() {
        applyTransition(currentState.handleStartMatching(context));
    }

    public void cancelMatching() {
        applyTransition(currentState.handleCancelMatching(context));
    }

    public void enterGame(GameSessionStateManager manager) {
        applyTransition(currentState.handleEnterGame(context, manager));
    }

    public void leaveGame() {
        applyTransition(currentState.handleLeaveGame(context));
    }

    public void disconnect() {
        applyTransition(currentState.handleDisconnect(context));
    }

    public void resetToConnected() {
        transitionTo(ClientStateType.CONNECTED);
    }

    private void applyTransition(ClientStateStep<?> step) {
        if (step == null || !step.hasTransition()) {
            return;
        }
        transitionTo(step.nextState());
    }

    private void transitionTo(ClientStateType targetType) {
        if (targetType == null) {
            return;
        }
        ClientState nextState = states.get(targetType);
        if (nextState == null) {
            throw new IllegalStateException("No client state registered for " + targetType);
        }
        if (currentState == nextState) {
            return;
        }
        currentState.onExit(context);
        currentState = nextState;
        currentState.onEnter(context);
    }
}
