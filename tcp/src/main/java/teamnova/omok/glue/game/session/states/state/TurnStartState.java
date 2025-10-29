package teamnova.omok.glue.game.session.states.state;

import java.util.Objects;

import teamnova.omok.glue.game.session.states.manage.GameSessionStateContext;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContextService;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateType;
import teamnova.omok.glue.rule.runtime.RuleRuntimeContext;
import teamnova.omok.modules.state_machine.interfaces.BaseState;
import teamnova.omok.modules.state_machine.interfaces.StateContext;
import teamnova.omok.modules.state_machine.models.StateName;
import teamnova.omok.modules.state_machine.models.StateStep;

/**
 * Marks the start of a new overall turn cycle and prepares runtime buffers.
 */
public final class TurnStartState implements BaseState {
    private final GameSessionStateContextService contextService;
    private final teamnova.omok.glue.game.session.model.dto.GameSessionServices services;

    public TurnStartState(GameSessionStateContextService contextService,
                          teamnova.omok.glue.game.session.model.dto.GameSessionServices services) {
        this.contextService = Objects.requireNonNull(contextService, "contextService");
        this.services = Objects.requireNonNull(services, "services");
    }

    @Override
    public StateName name() {
        return GameSessionStateType.TURN_START.toStateName();
    }

    @Override
    public <I extends StateContext> StateStep onEnter(I context) {
        GameSessionStateContext ctx = (GameSessionStateContext) context;
        // Fire TURN_ROUND_START so turn order rules can reseed order at the beginning of a round
        RuleRuntimeContext runtime = new teamnova.omok.glue.rule.runtime.RuleRuntimeContext(
            services,
            contextService,
            ctx,
            services.turnService().snapshot(ctx.turns()),
            teamnova.omok.glue.rule.api.RuleTriggerKind.TURN_ROUND_START
        );
        teamnova.omok.glue.game.session.services.RuleService ruleService = teamnova.omok.glue.game.session.services.RuleService.getInstance();
        ruleService.updateTurnBudget(ctx.rules(), runtime);
        ruleService.adjustTurnOrder(ctx.rules(), runtime);
        ruleService.activateRules(ctx.rules(), runtime);

        // Prepare personal turn frames and move to the first personal turn of this round
        contextService.turn().resetPersonalTurns(ctx);
        return StateStep.transition(GameSessionStateType.TURN_PERSONAL_START.toStateName());
    }
}
