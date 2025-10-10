package teamnova.omok.state.game.state;

import teamnova.omok.service.InGameSessionService;
import teamnova.omok.state.game.contract.GameSessionState;
import teamnova.omok.state.game.manage.GameSessionStateContext;
import teamnova.omok.state.game.manage.GameSessionStateStep;
import teamnova.omok.state.game.manage.GameSessionStateType;
import teamnova.omok.state.game.manage.TurnCycleContext;

/**
 * Determines whether the game concluded after the most recent move.
 */
public final class OutcomeEvaluatingState implements GameSessionState {
    @Override
    public GameSessionStateType type() {
        return GameSessionStateType.OUTCOME_EVALUATING;
    }

    @Override
    public GameSessionStateStep onEnter(GameSessionStateContext context) {
        TurnCycleContext cycle = context.activeTurnCycle();
        if (cycle == null) {
            return GameSessionStateStep.transition(GameSessionStateType.TURN_WAITING);
        }
        boolean finished = context.outcomeService()
            .handleStonePlaced(cycle.session(), cycle.userId(), cycle.x(), cycle.y(), cycle.stone());
        if (finished) {
            context.pendingMoveResult(InGameSessionService.MoveResult.success(
                cycle.session(),
                cycle.stone(),
                null,
                cycle.userId(),
                cycle.x(),
                cycle.y()
            ));
            context.clearTurnCycle();
            return GameSessionStateStep.transition(GameSessionStateType.COMPLETED);
        }
        return GameSessionStateStep.transition(GameSessionStateType.TURN_FINALIZING);
    }
}
