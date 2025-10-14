package teamnova.omok.glue.state.client.event;

import teamnova.omok.glue.state.client.manage.ClientStateContext;
import teamnova.omok.glue.state.client.manage.ClientStateStep;

@FunctionalInterface
public interface ClientEventHandler<E extends ClientEvent> {
    ClientStateStep handle(ClientStateContext context, E event);
}
