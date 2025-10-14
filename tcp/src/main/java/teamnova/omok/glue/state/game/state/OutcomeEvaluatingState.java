package teamnova.omok.glue.state.game.state;

import teamnova.omok.glue.service.dto.GameCompletionNotice;
import teamnova.omok.glue.service.dto.MoveResult;
import teamnova.omok.glue.state.game.manage.GameSessionStateContext;
import teamnova.omok.glue.state.game.manage.GameSessionStateType;
import teamnova.omok.glue.state.game.manage.TurnCycleContext;
import teamnova.omok.modules.state_machine.interfaces.BaseState;
import teamnova.omok.modules.state_machine.interfaces.StateContext;
import teamnova.omok.modules.state_machine.models.StateName;
import teamnova.omok.modules.state_machine.models.StateStep;

/**
 * Determines whether the game concluded after the most recent move.
 */
public final class OutcomeEvaluatingState implements BaseState {
    @Override
    public StateName name() {
        return GameSessionStateType.OUTCOME_EVALUATING.toStateName();
    }

    @Override
    public <I extends StateContext> StateStep onEnter(I context) {
        return onEnterInternal((GameSessionStateContext) context);
    }

    private StateStep onEnterInternal(GameSessionStateContext context) {
        TurnCycleContext cycle = context.activeTurnCycle();
        if (cycle == null) {
            return StateStep.transition(GameSessionStateType.TURN_WAITING.toStateName());
        }
        boolean finished = context.outcomeService()
            .handleStonePlaced(cycle.session(), cycle.userId(), cycle.x(), cycle.y(), cycle.stone());
        if (finished) {
            int turnCount;
            if (cycle.snapshots().current() != null) {
                turnCount = cycle.snapshots().current().turnNumber();
            } else {
                turnCount = cycle.session().getTurnStore().getTurnNumber();
            }
            cycle.session().lock().lock();
            try {
                cycle.session().markGameFinished(cycle.now(), turnCount);
            } finally {
                cycle.session().lock().unlock();
            }
            context.pendingMoveResult(MoveResult.success(
                cycle.session(),
                cycle.stone(),
                null,
                cycle.userId(),
                cycle.x(),
                cycle.y()
            ));
            context.pendingGameCompletion(new GameCompletionNotice(cycle.session()));
            context.clearTurnCycle();
            return StateStep.transition(GameSessionStateType.POST_GAME_DECISION_WAITING.toStateName());
        }
        return StateStep.transition(GameSessionStateType.TURN_FINALIZING.toStateName());
    }
}
