package teamnova.omok.glue.state.game.event;

import teamnova.omok.modules.state_machine.interfaces.BaseEvent;

public record MoveEvent(String userId, int x, int y, long timestamp, long requestId) implements BaseEvent {

}
