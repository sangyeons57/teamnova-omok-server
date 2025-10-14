package teamnova.omok.domain.session.client.event;

import teamnova.omok.domain.session.client.manage.ClientStateContext;
import teamnova.omok.domain.session.client.manage.ClientStateStep;

@FunctionalInterface
public interface ClientEventHandler<E extends ClientEvent> {
    ClientStateStep handle(ClientStateContext context, E event);
}
