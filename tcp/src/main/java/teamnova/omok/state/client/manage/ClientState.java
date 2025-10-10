package teamnova.omok.state.client.manage;

import teamnova.omok.state.game.manage.GameSessionStateManager;

/**
 * Contract for connection lifecycle states.
 */
public interface ClientState {
    ClientStateType type();

    default void onEnter(ClientStateContext context) { }

    default void onExit(ClientStateContext context) { }

    default ClientStateStep<Void> handleAuthenticated(ClientStateContext context) {
        return ClientStateStep.stay(null);
    }

    default ClientStateStep<Void> handleStartMatching(ClientStateContext context) {
        return ClientStateStep.stay(null);
    }

    default ClientStateStep<Void> handleCancelMatching(ClientStateContext context) {
        return ClientStateStep.stay(null);
    }

    default ClientStateStep<Void> handleEnterGame(ClientStateContext context,
                                                  GameSessionStateManager gameStateManager) {
        return ClientStateStep.stay(null);
    }

    default ClientStateStep<Void> handleLeaveGame(ClientStateContext context) {
        return ClientStateStep.stay(null);
    }

    default ClientStateStep<Void> handleDisconnect(ClientStateContext context) {
        return ClientStateStep.transition(null, ClientStateType.DISCONNECTED);
    }
}
