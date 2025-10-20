package teamnova.omok.glue.game.session.states.state;

import java.util.Objects;

import teamnova.omok.glue.game.session.interfaces.GameTurnService;
import teamnova.omok.glue.game.session.model.dto.GameSessionServices;
import teamnova.omok.glue.game.session.model.result.MoveResult;
import teamnova.omok.glue.game.session.model.runtime.TurnTransition;
import teamnova.omok.glue.game.session.services.RuleTurnStateView;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContext;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContextService;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateType;
import teamnova.omok.glue.game.session.states.manage.TurnCycleContext;
import teamnova.omok.glue.rule.RuleRuntimeContext;
import teamnova.omok.glue.rule.RuleTriggerKind;
import teamnova.omok.glue.rule.RulesContext;
import teamnova.omok.modules.state_machine.interfaces.BaseState;
import teamnova.omok.modules.state_machine.interfaces.StateContext;
import teamnova.omok.modules.state_machine.models.StateName;
import teamnova.omok.modules.state_machine.models.StateStep;

/**
 * Handles completion of the active player's turn and prepares information for subsequent stages.
 */
public final class TurnPersonalCompletedState implements BaseState {
    private final GameSessionStateContextService contextService;
    private final GameSessionServices services;
    private final GameTurnService turnService;

    public TurnPersonalCompletedState(GameSessionStateContextService contextService,
                                      GameSessionServices services) {
        this.contextService = Objects.requireNonNull(contextService, "contextService");
        this.services = Objects.requireNonNull(services, "services");
        this.turnService = Objects.requireNonNull(services.turnService(), "turnService");
    }

    @Override
    public StateName name() {
        return GameSessionStateType.TURN_PERSONAL_COMPLETED.toStateName();
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
        GameTurnService.TurnSnapshot nextSnapshot = turnService.advanceSkippingDisconnected(
            context.turns(),
            context.participants().disconnectedUsersView(),
            cycle.now()
        );
        cycle.snapshots().next(nextSnapshot);
        RuleTurnStateView turnStateView = RuleTurnStateView.fromAdvance(
            cycle.snapshots().current(),
            nextSnapshot,
            context.participants().getUserIds(),
            context.participants().disconnectedUsersView(),
            cycle.userId(),
            context.participants().playerIndexOf(cycle.userId())
        );
        TurnTransition transition = new TurnTransition(
            cycle.snapshots().current(),
            nextSnapshot,
            turnStateView
        );
        contextService.turn().recordTurnTransition(context, transition);
        fireRules(context, RuleTriggerKind.TURN_ADVANCE, turnStateView, GameSessionStateType.TURN_PERSONAL_COMPLETED);
        contextService.turn().queueMoveResult(context, MoveResult.success(
            cycle.stone(),
            nextSnapshot,
            cycle.userId(),
            cycle.x(),
            cycle.y()
        ));
        contextService.turn().clearTurnCycle(context);
        GameSessionStateType nextState = nextSnapshot.wrapped()
            ? GameSessionStateType.TURN_ROUND_COMPLETED
            : GameSessionStateType.TURN_STARTING;
        return StateStep.transition(nextState.toStateName());
    }

    private void fireRules(GameSessionStateContext context,
                           RuleTriggerKind trigger,
                           RuleTurnStateView view,
                           GameSessionStateType targetState) {
        RulesContext rulesContext = context.session().getRulesContext();
        if (rulesContext == null) {
            return;
        }
        RuleRuntimeContext runtime = new RuleRuntimeContext(
            services,
            contextService,
            context,
            view,
            trigger
        );
        rulesContext.activateRules(runtime, targetState);
    }
}
