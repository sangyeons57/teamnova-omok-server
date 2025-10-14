package teamnova.omok.glue.state.game.event;

import teamnova.omok.modules.state_machine.interfaces.BaseEvent;

public record DecisionTimeoutEvent(long triggerAt) implements BaseEvent {

}
