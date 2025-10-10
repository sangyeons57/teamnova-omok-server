package teamnova.omok.state.client.state;

import teamnova.omok.state.client.contract.ClientState;
import teamnova.omok.state.client.event.ClientEventRegistry;
import teamnova.omok.state.client.event.ClientEventType;
import teamnova.omok.state.client.event.DisconnectClientEvent;
import teamnova.omok.state.client.manage.ClientStateContext;
import teamnova.omok.state.client.manage.ClientStateStep;
import teamnova.omok.state.client.manage.ClientStateType;

/**
 * Terminal state once the client session has been closed.
 */
public final class DisconnectedClientState implements ClientState {
    @Override
    public ClientStateType type() {
        return ClientStateType.DISCONNECTED;
    }

    @Override
    public ClientStateStep onEnter(ClientStateContext context) {
        context.clearGame();
        return ClientStateStep.stay();
    }

    @Override
    public void registerHandlers(ClientEventRegistry registry) {
        registry.register(ClientEventType.DISCONNECT, DisconnectClientEvent.class,
            this::handleDisconnect);
    }

    private ClientStateStep handleDisconnect(ClientStateContext context,
                                             DisconnectClientEvent event) {
        return ClientStateStep.stay();
    }
}
