package teamnova.omok.glue.game.session.states.state;

import java.util.Objects;

import teamnova.omok.glue.game.session.model.dto.GameSessionServices;
import teamnova.omok.glue.game.session.services.RuleTurnStateView;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContext;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContextService;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContext.TurnTransition;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateType;
import teamnova.omok.glue.game.session.states.manage.GameSessionTurnContextService;
import teamnova.omok.glue.rule.RuleRuntimeContext;
import teamnova.omok.glue.rule.RuleTriggerKind;
import teamnova.omok.glue.rule.RulesContext;
import teamnova.omok.modules.state_machine.interfaces.BaseState;
import teamnova.omok.modules.state_machine.interfaces.StateContext;
import teamnova.omok.modules.state_machine.models.StateName;
import teamnova.omok.modules.state_machine.models.StateStep;

/**
 * Dispatches callbacks for the beginning of a new player's turn.
 */
public final class TurnStartingState implements BaseState {
    private final GameSessionStateContextService contextService;
    private final GameSessionTurnContextService turnContextService;
    private final GameSessionServices services;

    public TurnStartingState(GameSessionStateContextService contextService,
                             GameSessionServices services) {
        this.contextService = Objects.requireNonNull(contextService, "contextService");
        this.turnContextService = contextService.turn();
        this.services = Objects.requireNonNull(services, "services");
    }

    @Override
    public StateName name() {
        return GameSessionStateType.TURN_STARTING.toStateName();
    }

    @Override
    public <I extends StateContext> StateStep onEnter(I context) {
        GameSessionStateContext ctx = (GameSessionStateContext) context;
        TurnTransition transition = turnContextService.consumeTurnTransition(ctx);
        RuleTurnStateView view = buildView(ctx, transition);
        fireRules(ctx, view);
        return StateStep.transition(GameSessionStateType.TURN_WAITING.toStateName());
    }

    private RuleTurnStateView buildView(GameSessionStateContext context, TurnTransition transition) {
        if (transition != null && transition.nextSnapshot() != null) {
            return RuleTurnStateView.fromTurnStart(
                transition.nextSnapshot(),
                context.participants().getUserIds(),
                context.participants().disconnectedUsersView()
            );
        }
        return RuleTurnStateView.capture(context, services.turnService());
    }

    private void fireRules(GameSessionStateContext context, RuleTurnStateView view) {
        RulesContext rulesContext = context.session().getRulesContext();
        if (rulesContext == null) {
            return;
        }
        RuleRuntimeContext runtime = new RuleRuntimeContext(
            services,
            contextService,
            context,
            view,
            RuleTriggerKind.TURN_START
        );
        rulesContext.activateRules(runtime, GameSessionStateType.TURN_WAITING);
    }
}
