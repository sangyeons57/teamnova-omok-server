package teamnova.omok.domain.session.client.state;

import teamnova.omok.domain.session.client.contract.ClientState;
import teamnova.omok.domain.session.client.event.CancelMatchingClientEvent;
import teamnova.omok.domain.session.client.event.ClientEventRegistry;
import teamnova.omok.domain.session.client.event.ClientEventType;
import teamnova.omok.domain.session.client.event.DisconnectClientEvent;
import teamnova.omok.domain.session.client.event.EnterGameClientEvent;
import teamnova.omok.domain.session.client.manage.ClientStateContext;
import teamnova.omok.domain.session.client.manage.ClientStateStep;
import teamnova.omok.domain.session.client.manage.ClientStateType;

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
