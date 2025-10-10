package teamnova.omok.state.game.event;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

import teamnova.omok.state.game.manage.GameSessionStateContext;
import teamnova.omok.state.game.manage.GameSessionStateStep;

/**
 * Registry used by states to declare which events they can handle.
 */
public final class GameSessionEventRegistry {
    private final Map<GameSessionEventType, HandlerEntry> handlers =
        new EnumMap<>(GameSessionEventType.class);

    public <E extends GameSessionEvent> void register(GameSessionEventType type,
                                                      Class<E> eventClass,
                                                      GameSessionEventHandler<E> handler) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(eventClass, "eventClass");
        Objects.requireNonNull(handler, "handler");
        handlers.put(type, new HandlerEntry(eventClass, handler));
    }

    public Map<GameSessionEventType, HandlerEntry> handlers() {
        return Collections.unmodifiableMap(handlers);
    }

    public static final class HandlerEntry {
        private final Class<? extends GameSessionEvent> eventClass;
        private final GameSessionEventHandler<? extends GameSessionEvent> handler;

        HandlerEntry(Class<? extends GameSessionEvent> eventClass,
                     GameSessionEventHandler<? extends GameSessionEvent> handler) {
            this.eventClass = eventClass;
            this.handler = handler;
        }

        @SuppressWarnings("unchecked")
        public GameSessionStateStep invoke(GameSessionStateContext context, GameSessionEvent event) {
            return ((GameSessionEventHandler<GameSessionEvent>) handler)
                .handle(context, eventClass.cast(event));
        }
    }
}
