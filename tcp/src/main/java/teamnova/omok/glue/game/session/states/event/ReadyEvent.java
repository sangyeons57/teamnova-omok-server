package teamnova.omok.glue.game.session.states.event;

import teamnova.omok.modules.state_machine.interfaces.BaseEvent;

public record ReadyEvent(String userId, long timestamp, long requestId)
    implements BaseEvent {
}
