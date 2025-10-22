package teamnova.omok.glue.game.session.states.state;

import java.util.List;
import java.util.Objects;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionAccess;
import teamnova.omok.glue.game.session.model.dto.GameSessionServices;
import teamnova.omok.glue.game.session.model.dto.TurnSnapshot;
import teamnova.omok.glue.game.session.services.RuleService;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContext;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContextService;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateType;
import teamnova.omok.glue.game.session.states.manage.GameSessionTurnContextService;
import teamnova.omok.glue.manager.DataManager;
import teamnova.omok.glue.rule.RuleRuntimeContext;
import teamnova.omok.glue.rule.RuleTriggerKind;
import teamnova.omok.modules.state_machine.interfaces.BaseState;
import teamnova.omok.modules.state_machine.interfaces.StateContext;
import teamnova.omok.modules.state_machine.models.StateName;
import teamnova.omok.modules.state_machine.models.StateStep;

/**
 * Handles the boundary between personal turns, firing round-completion rules and
 * forwarding to the next overall turn or post-game flow.
 */
public final class TurnEndState implements BaseState {
    private final GameSessionStateContextService contextService;
    private final GameSessionTurnContextService turnContextService;
    private final GameSessionServices services;

    public TurnEndState(GameSessionStateContextService contextService,
                        GameSessionServices services) {
        this.contextService = Objects.requireNonNull(contextService, "contextService");
        this.turnContextService = contextService.turn();
        this.services = Objects.requireNonNull(services, "services");
    }

    @Override
    public StateName name() {
        return GameSessionStateType.TURN_END.toStateName();
    }

    @Override
    public <I extends StateContext> StateStep onEnter(I context) {
        GameSessionStateContext ctx = (GameSessionStateContext) context;
        if (ctx.outcomes().isGameFinished()) {
            applyScoreAdjustments(ctx);
            return StateStep.transition(GameSessionStateType.POST_GAME_DECISION_WAITING.toStateName());
        }
        TurnSnapshot snapshot = turnContextService.peekTurnSnapshot(ctx);
        if (snapshot != null && snapshot.wrapped()) {
            fireRules(ctx, snapshot);
        }
        return StateStep.transition(GameSessionStateType.TURN_START.toStateName());
    }

    private void fireRules(GameSessionStateContext context, TurnSnapshot snapshot) {
        RuleRuntimeContext runtime = new RuleRuntimeContext(
            services,
            contextService,
            context,
            snapshot,
            RuleTriggerKind.TURN_ROUND_COMPLETED
        );
        RuleService.getInstance().activateRules(context.rules(), runtime);
    }

    private void applyScoreAdjustments(GameSessionStateContext context) {
        GameSessionAccess session = context.session();
        List<String> users = context.participants().getUserIds();
        for (String userId : users) {
            int delta = services.scoreService().calculateScoreDelta(session, userId);
            if (delta != 0) {
                DataManager.getInstance().adjustUserScore(userId, delta);
            }
        }
    }
}
