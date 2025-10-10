package teamnova.omok.state.client;

import teamnova.omok.state.client.manage.ClientState;
import teamnova.omok.state.client.manage.ClientStateContext;
import teamnova.omok.state.client.manage.ClientStateStep;
import teamnova.omok.state.client.manage.ClientStateType;

/**
 * Terminal state once the client session has been closed.
 */
public class DisconnectedClientState implements ClientState {
    @Override
    public ClientStateType type() {
        return ClientStateType.DISCONNECTED;
    }

    @Override
    public void onEnter(ClientStateContext context) {
        context.clearGame();
    }

    @Override
    public ClientStateStep<Void> handleDisconnect(ClientStateContext context) {
        return ClientStateStep.stay(null);
    }
}
