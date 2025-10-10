package teamnova.omok.state.client.state;

import teamnova.omok.state.client.contract.ClientState;
import teamnova.omok.state.client.event.AuthenticatedClientEvent;
import teamnova.omok.state.client.event.ClientEventRegistry;
import teamnova.omok.state.client.event.ClientEventType;
import teamnova.omok.state.client.event.DisconnectClientEvent;
import teamnova.omok.state.client.manage.ClientStateContext;
import teamnova.omok.state.client.manage.ClientStateStep;
import teamnova.omok.state.client.manage.ClientStateType;

/**
 * Initial connection state before authentication.
 */
public final class ConnectedClientState implements ClientState {
    @Override
    public ClientStateType type() {
        return ClientStateType.CONNECTED;
    }

    @Override
    public void registerHandlers(ClientEventRegistry registry) {
        registry.register(ClientEventType.AUTHENTICATED, AuthenticatedClientEvent.class,
            this::handleAuthenticated);
        registry.register(ClientEventType.DISCONNECT, DisconnectClientEvent.class,
            this::handleDisconnect);
    }

    private ClientStateStep handleAuthenticated(ClientStateContext context,
                                                AuthenticatedClientEvent event) {
        return ClientStateStep.transition(ClientStateType.AUTHENTICATED);
    }

    private ClientStateStep handleDisconnect(ClientStateContext context,
                                             DisconnectClientEvent event) {
        context.clearGame();
        return ClientStateStep.transition(ClientStateType.DISCONNECTED);
    }
}
