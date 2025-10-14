package teamnova.omok.domain.session.game.entity.state.state;

import teamnova.omok.domain.session.game.entity.state.contract.GameSessionState;
import teamnova.omok.domain.session.game.entity.state.manage.GameSessionStateContext;
import teamnova.omok.domain.session.game.entity.state.manage.GameSessionStateStep;
import teamnova.omok.domain.session.game.entity.state.manage.GameSessionStateType;
import teamnova.omok.domain.session.game.entity.state.manage.TurnCycleContext;

/**
 * Writes the validated stone onto the board.
 */
public final class MoveApplyingState implements GameSessionState {
    @Override
    public GameSessionStateType type() {
        return GameSessionStateType.MOVE_APPLYING;
    }

    @Override
    public GameSessionStateStep onEnter(GameSessionStateContext context) {
        TurnCycleContext cycle = context.activeTurnCycle();
        if (cycle == null) {
            return GameSessionStateStep.transition(GameSessionStateType.TURN_WAITING);
        }
        context.session().setStone(
            cycle.x(),
            cycle.y(),
            cycle.stone()
        );
        return GameSessionStateStep.transition(GameSessionStateType.OUTCOME_EVALUATING);
    }
}
