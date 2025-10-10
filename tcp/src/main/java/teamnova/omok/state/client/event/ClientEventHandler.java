package teamnova.omok.state.client.event;

import teamnova.omok.state.client.manage.ClientStateContext;
import teamnova.omok.state.client.manage.ClientStateStep;

@FunctionalInterface
public interface ClientEventHandler<E extends ClientEvent> {
    ClientStateStep handle(ClientStateContext context, E event);
}
