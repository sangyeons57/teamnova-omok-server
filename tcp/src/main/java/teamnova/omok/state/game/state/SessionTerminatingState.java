package teamnova.omok.state.game.state;

import teamnova.omok.state.game.contract.GameSessionState;
import teamnova.omok.state.game.event.GameSessionEventRegistry;
import teamnova.omok.state.game.manage.GameSessionStateContext;
import teamnova.omok.state.game.manage.GameSessionStateStep;
import teamnova.omok.state.game.manage.GameSessionStateType;

/**
 * Ensures decision timers are cleared before the session is fully terminated.
 */
public final class SessionTerminatingState implements GameSessionState {
    @Override
    public GameSessionStateType type() {
        return GameSessionStateType.SESSION_TERMINATING;
    }

    @Override
    public void registerHandlers(GameSessionEventRegistry registry) {
        // no additional events to process; service will remove the session.
    }

    @Override
    public GameSessionStateStep onEnter(GameSessionStateContext context) {
        context.clearPostGameDecisionDeadline();
        return GameSessionStateStep.transition(GameSessionStateType.COMPLETED);
    }
}
