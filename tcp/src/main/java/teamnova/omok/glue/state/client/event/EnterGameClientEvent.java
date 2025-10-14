package teamnova.omok.glue.state.client.event;

import java.util.Objects;

import teamnova.omok.glue.state.game.GameStateHub;
import teamnova.omok.modules.state_machine.interfaces.BaseEvent;

public record EnterGameClientEvent(GameStateHub gameStateManager) implements BaseEvent {

}
