package teamnova.omok.domain.session.game.entity.state.state;

import teamnova.omok.domain.session.game.entity.state.contract.GameSessionState;
import teamnova.omok.domain.session.game.entity.state.manage.GameSessionStateContext;
import teamnova.omok.domain.session.game.entity.state.manage.GameSessionStateStep;
import teamnova.omok.domain.session.game.entity.state.manage.GameSessionStateType;

/**
 * Ensures decision timers are cleared before the session is fully terminated.
 */
public final class SessionTerminatingState implements GameSessionState {
    @Override
    public GameSessionStateType type() {
        return GameSessionStateType.SESSION_TERMINATING;
    }

    @Override
    public GameSessionStateStep onEnter(GameSessionStateContext context) {
        context.clearPostGameDecisionDeadline();
        return GameSessionStateStep.transition(GameSessionStateType.COMPLETED);
    }
}
