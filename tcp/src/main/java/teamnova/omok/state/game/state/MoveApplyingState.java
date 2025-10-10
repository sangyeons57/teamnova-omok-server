package teamnova.omok.state.game.state;

import teamnova.omok.state.game.contract.GameSessionState;
import teamnova.omok.state.game.manage.GameSessionStateContext;
import teamnova.omok.state.game.manage.GameSessionStateStep;
import teamnova.omok.state.game.manage.GameSessionStateType;
import teamnova.omok.state.game.manage.TurnCycleContext;

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
        context.boardService().setStone(
            context.session().getBoardStore(),
            cycle.x(),
            cycle.y(),
            cycle.stone()
        );
        return GameSessionStateStep.transition(GameSessionStateType.OUTCOME_EVALUATING);
    }
}
