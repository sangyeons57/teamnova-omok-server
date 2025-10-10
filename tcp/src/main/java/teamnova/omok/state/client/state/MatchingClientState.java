package teamnova.omok.state.client.state;

import teamnova.omok.state.client.contract.ClientState;
import teamnova.omok.state.client.event.CancelMatchingClientEvent;
import teamnova.omok.state.client.event.ClientEventRegistry;
import teamnova.omok.state.client.event.ClientEventType;
import teamnova.omok.state.client.event.DisconnectClientEvent;
import teamnova.omok.state.client.event.EnterGameClientEvent;
import teamnova.omok.state.client.manage.ClientStateContext;
import teamnova.omok.state.client.manage.ClientStateStep;
import teamnova.omok.state.client.manage.ClientStateType;

/**
 * State while the client is queued for matchmaking.
 */
public final class MatchingClientState implements ClientState {
    @Override
    public ClientStateType type() {
        return ClientStateType.MATCHING;
    }

    @Override
    public void registerHandlers(ClientEventRegistry registry) {
        registry.register(ClientEventType.CANCEL_MATCHING, CancelMatchingClientEvent.class,
            this::handleCancelMatching);
        registry.register(ClientEventType.ENTER_GAME, EnterGameClientEvent.class,
            this::handleEnterGame);
        registry.register(ClientEventType.DISCONNECT, DisconnectClientEvent.class,
            this::handleDisconnect);
    }

    private ClientStateStep handleCancelMatching(ClientStateContext context,
                                                 CancelMatchingClientEvent event) {
        return ClientStateStep.transition(ClientStateType.AUTHENTICATED);
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
