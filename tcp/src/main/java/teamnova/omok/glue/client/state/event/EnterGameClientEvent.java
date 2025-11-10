package teamnova.omok.glue.client.state.event;

import teamnova.omok.glue.game.session.states.GameStateHub;
import teamnova.omok.modules.state_machine.interfaces.BaseEvent;

public record EnterGameClientEvent(GameStateHub gameStateManager) implements BaseEvent {

}
