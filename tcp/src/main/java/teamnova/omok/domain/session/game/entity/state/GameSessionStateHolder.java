package teamnova.omok.domain.session.game.entity.state;

import teamnova.omok.domain.session.game.entity.state.contract.GameSessionEvent;
import teamnova.omok.domain.session.game.entity.state.contract.GameSessionState;
import teamnova.omok.domain.session.game.entity.state.manage.GameSessionStateContext;
import teamnova.omok.domain.session.game.entity.state.manage.GameSessionStateType;

import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

public class GameSessionStateHolder {
    private GameSessionStateType type;
    private final Queue<PendingEvent> eventQueue = new ConcurrentLinkedQueue<>();

    public GameSessionStateType currentType() {
        return type;
    }

    public void setCurrentType(GameSessionStateType currentType) {
        this.type = currentType;
    }

    public GameSessionStateType getType() {
        return type;
    }

    public void submitEvent(GameSessionEvent event, Consumer<GameSessionStateContext> callback) {
        Objects.requireNonNull(event, "event");
        eventQueue.add(new PendingEvent(event, callback));
    }

    public PendingEvent poll() {
        return eventQueue.poll();
    }

    public record PendingEvent(GameSessionEvent event,
                                Consumer<GameSessionStateContext> callback) {
        public GameSessionEvent event() {
            return event;
        }
        public Consumer<GameSessionStateContext> callback() {
            return callback;
        }
    }
}
