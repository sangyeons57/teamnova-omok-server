package teamnova.omok.glue.state.client.event;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

import teamnova.omok.glue.state.client.manage.ClientStateContext;
import teamnova.omok.glue.state.client.manage.ClientStateStep;

/**
 * Registry used by client states to declare which events they handle.
 */
public final class ClientEventRegistry {
    private final Map<ClientEventType, HandlerEntry> handlers =
        new EnumMap<>(ClientEventType.class);

    public <E extends ClientEvent> void register(ClientEventType type,
                                                 Class<E> eventClass,
                                                 ClientEventHandler<E> handler) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(eventClass, "eventClass");
        Objects.requireNonNull(handler, "handler");
        handlers.put(type, new HandlerEntry(eventClass, handler));
    }

    public Map<ClientEventType, HandlerEntry> handlers() {
        return Collections.unmodifiableMap(handlers);
    }

    public static final class HandlerEntry {
        private final Class<? extends ClientEvent> eventClass;
        private final ClientEventHandler<? extends ClientEvent> handler;

        HandlerEntry(Class<? extends ClientEvent> eventClass,
                     ClientEventHandler<? extends ClientEvent> handler) {
            this.eventClass = eventClass;
            this.handler = handler;
        }

        @SuppressWarnings("unchecked")
        public ClientStateStep invoke(ClientStateContext context, ClientEvent event) {
            return ((ClientEventHandler<ClientEvent>) handler)
                .handle(context, eventClass.cast(event));
        }
    }
}
