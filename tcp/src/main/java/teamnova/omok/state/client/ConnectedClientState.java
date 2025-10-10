package teamnova.omok.state.client;

import teamnova.omok.state.client.manage.ClientState;
import teamnova.omok.state.client.manage.ClientStateContext;
import teamnova.omok.state.client.manage.ClientStateStep;
import teamnova.omok.state.client.manage.ClientStateType;

/**
 * Initial connection state before authentication.
 */
public class ConnectedClientState implements ClientState {
    @Override
    public ClientStateType type() {
        return ClientStateType.CONNECTED;
    }

    @Override
    public ClientStateStep<Void> handleAuthenticated(ClientStateContext context) {
        return ClientStateStep.transition(null, ClientStateType.AUTHENTICATED);
    }
}
