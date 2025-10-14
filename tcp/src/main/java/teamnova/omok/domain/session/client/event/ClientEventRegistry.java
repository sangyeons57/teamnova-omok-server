package teamnova.omok.domain.session.client.event;

import java.util.*;

/**
 * Registry used by client states to declare which events they handle.
 */
public final class ClientEventRegistry {
    private final Map<ClientEventType, Set<ClientEventType>> handlers =
        new EnumMap<>(ClientEventType.class);

    public <E extends ClientEvent> void register(ClientEventType type,
                                                 ClientEventHandler<E> handler) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(handler, "handler");
        handlers.put(type, handler);
    }

    public Set<ClientEventType> handlers() {
        return Collections.unmodifiableSet(handlers);
    }
}
