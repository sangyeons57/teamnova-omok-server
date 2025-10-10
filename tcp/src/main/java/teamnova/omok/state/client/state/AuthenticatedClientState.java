package teamnova.omok.state.client.state;

import teamnova.omok.state.client.contract.ClientState;
import teamnova.omok.state.client.event.ClientEventRegistry;
import teamnova.omok.state.client.event.ClientEventType;
import teamnova.omok.state.client.event.DisconnectClientEvent;
import teamnova.omok.state.client.event.EnterGameClientEvent;
import teamnova.omok.state.client.event.StartMatchingClientEvent;
import teamnova.omok.state.client.manage.ClientStateContext;
import teamnova.omok.state.client.manage.ClientStateStep;
import teamnova.omok.state.client.manage.ClientStateType;

/**
 * State once the client has successfully authenticated but is not yet in a game.
 */
public final class AuthenticatedClientState implements ClientState {
    @Override
    public ClientStateType type() {
        return ClientStateType.AUTHENTICATED;
    }

    @Override
    public void registerHandlers(ClientEventRegistry registry) {
        registry.register(ClientEventType.START_MATCHING, StartMatchingClientEvent.class,
            this::handleStartMatching);
        registry.register(ClientEventType.ENTER_GAME, EnterGameClientEvent.class,
            this::handleEnterGame);
        registry.register(ClientEventType.DISCONNECT, DisconnectClientEvent.class,
            this::handleDisconnect);
    }

    private ClientStateStep handleStartMatching(ClientStateContext context,
                                                StartMatchingClientEvent event) {
        return ClientStateStep.transition(ClientStateType.MATCHING);
    }

    private ClientStateStep handleEnterGame(ClientStateContext context,
                                            EnterGameClientEvent event) {
        context.attachGame(event.gameStateManager());
        return ClientStateStep.transition(ClientStateType.IN_GAME);
    }

    private ClientStateStep handleDisconnect(ClientStateContext context,
                                             DisconnectClientEvent event) {
        context.clearGame();
        return ClientStateStep.transition(ClientStateType.DISCONNECTED);
    }
}
