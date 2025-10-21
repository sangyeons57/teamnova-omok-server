package teamnova.omok.glue.game.session.states.state;

import java.util.Objects;

import teamnova.omok.glue.game.session.interfaces.GameTurnService;
import teamnova.omok.glue.game.session.model.dto.GameSessionServices;
import teamnova.omok.glue.game.session.model.dto.TurnSnapshot;
import teamnova.omok.glue.game.session.services.RuleService;
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
 * Handles the start of an individual player's turn within an overall turn cycle.
 */
public final class TurnPersonalStartingState implements BaseState {
    private final GameSessionStateContextService contextService;
    private final GameSessionTurnContextService turnContextService;
    private final GameSessionServices services;

    public TurnPersonalStartingState(GameSessionStateContextService contextService,
                                     GameSessionServices services) {
        this.contextService = Objects.requireNonNull(contextService, "contextService");
        this.turnContextService = contextService.turn();
        this.services = Objects.requireNonNull(services, "services");
    }

    @Override
    public StateName name() {
        return GameSessionStateType.TURN_PERSONAL_STARTING.toStateName();
    }

    @Override
    public <I extends StateContext> StateStep onEnter(I context) {
        GameSessionStateContext ctx = (GameSessionStateContext) context;
        TurnSnapshot snapshot = turnContextService.consumeTurnSnapshot(ctx);
        if (snapshot == null) {
            snapshot = services.turnService().snapshot(ctx.turns());
        }
        if (snapshot == null) {
            throw new IllegalStateException("No turn snapshot available for personal turn start");
        }
        turnContextService.beginPersonalTurn(ctx, snapshot, System.currentTimeMillis());
        fireRules(ctx, snapshot);
        return StateStep.transition(GameSessionStateType.TURN_WAITING.toStateName());
    }

    private void fireRules(GameSessionStateContext context, TurnSnapshot snapshot) {
        RuleRuntimeContext runtime = new RuleRuntimeContext(
            services,
            contextService,
            context,
            snapshot,
            RuleTriggerKind.TURN_START
        );
        RuleService.getInstance().activateRules(context.rules(), runtime);
    }
}
