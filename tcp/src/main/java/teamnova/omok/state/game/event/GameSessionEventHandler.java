package teamnova.omok.state.game.event;

import teamnova.omok.state.game.manage.GameSessionStateContext;
import teamnova.omok.state.game.manage.GameSessionStateStep;

@FunctionalInterface
public interface GameSessionEventHandler<E extends GameSessionEvent> {
    GameSessionStateStep handle(GameSessionStateContext context, E event);
}
