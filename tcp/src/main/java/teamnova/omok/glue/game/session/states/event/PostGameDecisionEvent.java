package teamnova.omok.glue.game.session.states.event;

import teamnova.omok.glue.game.session.model.PostGameDecision;
import teamnova.omok.modules.state_machine.interfaces.BaseEvent;

public record PostGameDecisionEvent(String userId,
                                    PostGameDecision decision,
                                    long timestamp,
                                    long requestId) implements BaseEvent {

}
