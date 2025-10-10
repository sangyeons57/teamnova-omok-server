package teamnova.omok.state.client;

import teamnova.omok.state.client.manage.ClientState;
import teamnova.omok.state.client.manage.ClientStateContext;
import teamnova.omok.state.client.manage.ClientStateStep;
import teamnova.omok.state.client.manage.ClientStateType;

/**
 * State while the client participates in an in-progress game.
 */
public class InGameClientState implements ClientState {
    @Override
    public ClientStateType type() {
        return ClientStateType.IN_GAME;
    }

    @Override
    public void onExit(ClientStateContext context) {
        context.clearGame();
    }

    @Override
    public ClientStateStep<Void> handleLeaveGame(ClientStateContext context) {
        return ClientStateStep.transition(null, ClientStateType.AUTHENTICATED);
    }

    @Override
    public ClientStateStep<Void> handleCancelMatching(ClientStateContext context) {
        return ClientStateStep.stay(null);
    }

    @Override
    public ClientStateStep<Void> handleStartMatching(ClientStateContext context) {
        return ClientStateStep.stay(null);
    }

    @Override
    public ClientStateStep<Void> handleDisconnect(ClientStateContext context) {
        context.clearGame();
        return ClientState.super.handleDisconnect(context);
    }
}
