package teamnova.omok.state.client.contract;

import teamnova.omok.state.client.event.ClientEventRegistry;
import teamnova.omok.state.client.manage.ClientStateContext;
import teamnova.omok.state.client.manage.ClientStateStep;
import teamnova.omok.state.client.manage.ClientStateType;

/**
 * Base contract implemented by all client session states.
 */
public interface ClientState {
    ClientStateType type();

    default ClientStateStep onEnter(ClientStateContext context) {
        return ClientStateStep.stay();
    }

    default void onExit(ClientStateContext context) { }

    default ClientStateStep onUpdate(ClientStateContext context, long now) {
        return ClientStateStep.stay();
    }

    default void registerHandlers(ClientEventRegistry registry) { }
}
