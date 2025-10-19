package teamnova.omok.glue.game.session.states.state;

import java.util.Objects;

import teamnova.omok.glue.game.session.interfaces.GameTurnService;
import teamnova.omok.glue.game.session.model.result.MoveResult;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContext;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContextService;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateType;
import teamnova.omok.glue.game.session.states.manage.TurnCycleContext;
import teamnova.omok.modules.state_machine.interfaces.BaseState;
import teamnova.omok.modules.state_machine.interfaces.StateContext;
import teamnova.omok.modules.state_machine.models.StateName;
import teamnova.omok.modules.state_machine.models.StateStep;

/**
 * Advances the turn order when the game continues.
 */
public final class TurnFinalizingState implements BaseState {
    private final GameSessionStateContextService contextService;
    private final GameTurnService turnService;

    public TurnFinalizingState(GameSessionStateContextService contextService,
                               GameTurnService turnService) {
        this.contextService = Objects.requireNonNull(contextService, "contextService");
        this.turnService = Objects.requireNonNull(turnService, "turnService");
    }
    @Override
    public StateName name() {
        return GameSessionStateType.TURN_FINALIZING.toStateName();
    }

    @Override
    public <I extends StateContext> StateStep onEnter(I context) {
        return onEnterInternal((GameSessionStateContext) context);
    }

    private StateStep onEnterInternal(GameSessionStateContext context) {
        TurnCycleContext cycle = contextService.turn().activeTurnCycle(context);
        if (cycle == null) {
            return StateStep.transition(GameSessionStateType.TURN_WAITING.toStateName());
        }
        GameTurnService.TurnSnapshot nextSnapshot = turnService
            .advanceSkippingDisconnected(
                context.turns(),
                context.participants().disconnectedUsersView(),
                cycle.now()
            );
        cycle.snapshots().next(nextSnapshot);
        contextService.turn().queueMoveResult(context, MoveResult.success(
            cycle.session(),
            cycle.stone(),
            nextSnapshot,
            cycle.userId(),
            cycle.x(),
            cycle.y()
        ));
        contextService.turn().clearTurnCycle(context);
        return StateStep.transition(GameSessionStateType.TURN_WAITING.toStateName());
    }
}
