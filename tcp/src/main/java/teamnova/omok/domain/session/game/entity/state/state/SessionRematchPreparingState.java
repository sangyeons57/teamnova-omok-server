package teamnova.omok.domain.session.game.entity.state.state;

import teamnova.omok.domain.session.game.entity.state.contract.GameSessionState;
import teamnova.omok.domain.session.game.entity.state.manage.GameSessionStateContext;
import teamnova.omok.domain.session.game.entity.state.manage.GameSessionStateStep;
import teamnova.omok.domain.session.game.entity.state.manage.GameSessionStateType;

/**
 * Finalizes the current session before rematch participants move to a new session.
 */
public final class SessionRematchPreparingState implements GameSessionState {
    @Override
    public GameSessionStateType type() {
        return GameSessionStateType.SESSION_REMATCH_PREPARING;
    }

    @Override
    public GameSessionStateStep onEnter(GameSessionStateContext context) {
        context.clearPostGameDecisionDeadline();
        return GameSessionStateStep.transition(GameSessionStateType.COMPLETED);
    }
}
