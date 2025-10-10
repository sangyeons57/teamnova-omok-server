package teamnova.omok.state.game.state;

import teamnova.omok.service.InGameSessionService;
import teamnova.omok.service.TurnService;
import teamnova.omok.state.game.contract.GameSessionState;
import teamnova.omok.state.game.manage.GameSessionStateContext;
import teamnova.omok.state.game.manage.GameSessionStateStep;
import teamnova.omok.state.game.manage.GameSessionStateType;
import teamnova.omok.state.game.manage.TurnCycleContext;

/**
 * Advances the turn order when the game continues.
 */
public final class TurnFinalizingState implements GameSessionState {
    @Override
    public GameSessionStateType type() {
        return GameSessionStateType.TURN_FINALIZING;
    }

    @Override
    public GameSessionStateStep onEnter(GameSessionStateContext context) {
        TurnCycleContext cycle = context.activeTurnCycle();
        if (cycle == null) {
            return GameSessionStateStep.transition(GameSessionStateType.TURN_WAITING);
        }
        TurnService.TurnSnapshot nextSnapshot = context.turnService()
            .advance(context.session().getTurnStore(), context.session().getUserIds(), cycle.now());
        cycle.snapshots().next(nextSnapshot);
        context.pendingMoveResult(InGameSessionService.MoveResult.success(
            cycle.session(),
            cycle.stone(),
            nextSnapshot,
            cycle.userId(),
            cycle.x(),
            cycle.y()
        ));
        context.clearTurnCycle();
        return GameSessionStateStep.transition(GameSessionStateType.TURN_WAITING);
    }
}
