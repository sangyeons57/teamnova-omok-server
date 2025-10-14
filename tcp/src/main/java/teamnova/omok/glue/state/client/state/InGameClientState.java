package teamnova.omok.glue.state.client.state;

import teamnova.omok.glue.state.client.contract.ClientState;
import teamnova.omok.glue.state.client.event.ClientEventRegistry;
import teamnova.omok.glue.state.client.event.ClientEventType;
import teamnova.omok.glue.state.client.event.DisconnectClientEvent;
import teamnova.omok.glue.state.client.event.LeaveGameClientEvent;
import teamnova.omok.glue.state.client.manage.ClientStateContext;
import teamnova.omok.glue.state.client.manage.ClientStateStep;
import teamnova.omok.glue.state.client.manage.ClientStateType;

/**
 * State while the client participates in an in-progress game.
 */
public final class InGameClientState implements ClientState {
    @Override
    public ClientStateType type() {
        return ClientStateType.IN_GAME;
    }

    @Override
    public void onExit(ClientStateContext context) {
        context.clearGame();
    }

    @Override
    public void registerHandlers(ClientEventRegistry registry) {
        registry.register(ClientEventType.LEAVE_GAME, LeaveGameClientEvent.class,
            this::handleLeaveGame);
        registry.register(ClientEventType.DISCONNECT, DisconnectClientEvent.class,
            this::handleDisconnect);
    }

    private ClientStateStep handleLeaveGame(ClientStateContext context,
                                            LeaveGameClientEvent event) {
        return ClientStateStep.transition(ClientStateType.AUTHENTICATED);
    }

    private ClientStateStep handleDisconnect(ClientStateContext context,
                                             DisconnectClientEvent event) {
        return ClientStateStep.transition(ClientStateType.DISCONNECTED);
    }
}
