package teamnova.omok.glue.game.session.states.event;

import teamnova.omok.modules.state_machine.interfaces.BaseEvent;

public record TimeoutEvent(int expectedTurnNumber, long timestamp) implements BaseEvent {
}
