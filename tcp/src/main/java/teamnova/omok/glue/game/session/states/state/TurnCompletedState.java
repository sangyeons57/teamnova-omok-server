package teamnova.omok.glue.game.session.states.state;

import java.util.Objects;

import teamnova.omok.glue.game.session.model.dto.GameSessionServices;
import teamnova.omok.glue.game.session.model.runtime.TurnTransition;
import teamnova.omok.glue.game.session.services.RuleService;
import teamnova.omok.glue.game.session.services.RuleTurnStateView;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContext;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContextService;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateType;
import teamnova.omok.glue.game.session.states.manage.GameSessionTurnContextService;
import teamnova.omok.glue.rule.RuleRuntimeContext;
import teamnova.omok.glue.rule.RuleTriggerKind;
import teamnova.omok.modules.state_machine.interfaces.BaseState;
import teamnova.omok.modules.state_machine.interfaces.StateContext;
import teamnova.omok.modules.state_machine.models.StateName;
import teamnova.omok.modules.state_machine.models.StateStep;

/**
 * Emits rule callbacks when an entire round completes (all players have acted once).
 */
public final class TurnCompletedState implements BaseState {
    private final GameSessionStateContextService contextService;
    private final GameSessionTurnContextService turnContextService;
    private final GameSessionServices services;

    public TurnCompletedState(GameSessionStateContextService contextService,
                              GameSessionServices services) {
        this.contextService = Objects.requireNonNull(contextService, "contextService");
        this.turnContextService = contextService.turn();
        this.services = Objects.requireNonNull(services, "services");
    }

    @Override
    public StateName name() {
        return GameSessionStateType.TURN_ROUND_COMPLETED.toStateName();
    }

    @Override
    public <I extends StateContext> StateStep onEnter(I context) {
        GameSessionStateContext ctx = (GameSessionStateContext) context;
        TurnTransition transition = turnContextService.peekTurnTransition(ctx);
        if (transition == null || !transition.roundWrapped()) {
            return StateStep.transition(GameSessionStateType.TURN_STARTING.toStateName());
        }
        RuleTurnStateView view = ensureView(transition);
        fireRules(ctx, view);
        return StateStep.transition(GameSessionStateType.TURN_STARTING.toStateName());
    }

    private RuleTurnStateView ensureView(TurnTransition transition) {
        RuleTurnStateView view = transition.view();
        if (view != null) {
            return view;
        }
        return RuleTurnStateView.fromAdvance(transition.currentSnapshot(), transition.nextSnapshot());
    }

    private void fireRules(GameSessionStateContext context, RuleTurnStateView view) {
        RuleRuntimeContext runtime = new RuleRuntimeContext(
            services,
            contextService,
            context,
            view,
            RuleTriggerKind.TURN_ROUND_COMPLETED
        );
        RuleService.getInstance().activateRules(context.rules(), runtime);
    }
}
