package teamnova.omok.state.game.contract;

import teamnova.omok.state.game.event.GameSessionEventRegistry;
import teamnova.omok.state.game.manage.GameSessionStateContext;
import teamnova.omok.state.game.manage.GameSessionStateStep;
import teamnova.omok.state.game.manage.GameSessionStateType;

/**
 * Base contract implemented by all game session states.
 */
public interface GameSessionState {
    GameSessionStateType type();

    default GameSessionStateStep onEnter(GameSessionStateContext context) {
        return GameSessionStateStep.stay();
    }

    default void onExit(GameSessionStateContext context) { }

    default GameSessionStateStep onUpdate(GameSessionStateContext context, long now) {
        return GameSessionStateStep.stay();
    }

    default void registerHandlers(GameSessionEventRegistry registry) { }
}
