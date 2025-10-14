package teamnova.omok.glue.state.game.event;

import teamnova.omok.glue.game.PostGameDecision;
import teamnova.omok.modules.state_machine.interfaces.BaseEvent;

public record PostGameDecisionEvent(String userId,
                                    PostGameDecision decision,
                                    long timestamp,
                                    long requestId) implements BaseEvent {

}
