package teamnova.omok.domain.session.game.entity.state.contract;

import teamnova.omok.domain.session.game.entity.state.manage.GameSessionStateContext;
import teamnova.omok.domain.session.game.entity.state.manage.GameSessionStateStep;
import teamnova.omok.domain.session.game.entity.state.manage.GameSessionStateType;

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

    default GameSessionStateStep onEvent(GameSessionStateContext context, GameSessionEvent event) {
        return GameSessionStateStep.stay();
    }
}
