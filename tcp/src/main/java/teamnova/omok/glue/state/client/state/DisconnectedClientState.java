package teamnova.omok.glue.state.client.state;

import teamnova.omok.glue.state.client.contract.ClientState;
import teamnova.omok.glue.state.client.event.ClientEventRegistry;
import teamnova.omok.glue.state.client.event.ClientEventType;
import teamnova.omok.glue.state.client.event.DisconnectClientEvent;
import teamnova.omok.glue.state.client.manage.ClientStateContext;
import teamnova.omok.glue.state.client.manage.ClientStateStep;
import teamnova.omok.glue.state.client.manage.ClientStateType;

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
