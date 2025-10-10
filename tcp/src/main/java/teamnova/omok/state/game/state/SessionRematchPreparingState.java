package teamnova.omok.state.game.state;

import teamnova.omok.state.game.contract.GameSessionState;
import teamnova.omok.state.game.event.GameSessionEventRegistry;
import teamnova.omok.state.game.manage.GameSessionStateContext;
import teamnova.omok.state.game.manage.GameSessionStateStep;
import teamnova.omok.state.game.manage.GameSessionStateType;

/**
 * Finalizes the current session before rematch participants move to a new session.
 */
public final class SessionRematchPreparingState implements GameSessionState {
    @Override
    public GameSessionStateType type() {
        return GameSessionStateType.SESSION_REMATCH_PREPARING;
    }

    @Override
    public void registerHandlers(GameSessionEventRegistry registry) {
        // no events handled; service performs the rematch hand-off.
    }

    @Override
    public GameSessionStateStep onEnter(GameSessionStateContext context) {
        context.clearPostGameDecisionDeadline();
        return GameSessionStateStep.transition(GameSessionStateType.COMPLETED);
    }
}
