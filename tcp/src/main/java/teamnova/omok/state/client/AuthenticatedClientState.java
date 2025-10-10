package teamnova.omok.state.client;

import teamnova.omok.state.client.manage.ClientState;
import teamnova.omok.state.client.manage.ClientStateContext;
import teamnova.omok.state.client.manage.ClientStateStep;
import teamnova.omok.state.client.manage.ClientStateType;
import teamnova.omok.state.game.manage.GameSessionStateManager;

/**
 * State once the client has successfully authenticated but is not yet in a game.
 */
public class AuthenticatedClientState implements ClientState {
    @Override
    public ClientStateType type() {
        return ClientStateType.AUTHENTICATED;
    }

    @Override
    public ClientStateStep<Void> handleStartMatching(ClientStateContext context) {
        return ClientStateStep.transition(null, ClientStateType.MATCHING);
    }

    @Override
    public ClientStateStep<Void> handleEnterGame(ClientStateContext context,
                                                 GameSessionStateManager gameStateManager) {
        context.attachGame(gameStateManager);
        return ClientStateStep.transition(null, ClientStateType.IN_GAME);
    }

    @Override
    public ClientStateStep<Void> handleDisconnect(ClientStateContext context) {
        context.clearGame();
        return ClientState.super.handleDisconnect(context);
    }
}
