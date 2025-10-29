package teamnova.omok.glue.game.session.states.state;

import java.util.List;
import java.util.Objects;

import teamnova.omok.glue.client.session.ClientSessionManager;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionAccess;
import teamnova.omok.glue.game.session.model.PlayerResult;
import teamnova.omok.glue.game.session.model.dto.GameSessionServices;
import teamnova.omok.glue.game.session.model.dto.TurnSnapshot;
import teamnova.omok.glue.game.session.model.messages.GameCompletionNotice;
import teamnova.omok.glue.game.session.services.RuleService;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContext;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContextService;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateType;
import teamnova.omok.glue.game.session.states.manage.GameSessionTurnContextService;
import teamnova.omok.glue.manager.DataManager;
import teamnova.omok.glue.rule.runtime.RuleRuntimeContext;
import teamnova.omok.glue.rule.api.RuleTriggerKind;
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
        AbandonmentResult abandonment = handleAbandonmentIfNeeded(ctx);
        if (abandonment != null) {
            if (abandonment.requiresFinalize()) {
                evaluateOutcomeRules(ctx);
                if (ctx.outcomes().isGameFinished()) {
                    return finalizeSession(ctx);
                }
            }
            return abandonment.step();
        }

        evaluateOutcomeRules(ctx);
        if (ctx.outcomes().isGameFinished()) {
            return finalizeSession(ctx);
        }

        TurnSnapshot snapshot = turnContextService.peekTurnSnapshot(ctx);
        if (snapshot != null && snapshot.wrapped()) {
            fireRules(ctx, snapshot);
        }
        return StateStep.transition(GameSessionStateType.TURN_START.toStateName());
    }

    private AbandonmentResult handleAbandonmentIfNeeded(GameSessionStateContext ctx) {
        var session = ctx.session();
        int total = ctx.participants().getUserIds().size();
        int dc = session.disconnectedUsersView().size();
        int connected = total - dc;

        if (connected <= 0) {
            // All participants left: terminate session immediately
            services.turnTimeoutScheduler().cancel(session.sessionId());
            services.decisionTimeoutScheduler().cancel(session.sessionId());
            services.messenger().broadcastSessionTerminated(session, List.copyOf(session.disconnectedUsersView()));
            services.repository().removeById(session.sessionId()).ifPresent(services.runtime()::remove);

            for (String uid : session.getUserIds()) {
                ClientSessionManager.getInstance()
                        .findSession(uid)
                        .ifPresent(h -> h.unbindGameSession(session.sessionId()));
            }

            // Session runtime removed; stay indicates no further transition within this state machine tick
            return new AbandonmentResult(StateStep.stay(), false);
        }

        if (connected == 1 && !ctx.outcomes().isGameFinished()) {
            // Exactly one participant remains: award forfeit win
            String winner = null;
            for (String uid : ctx.participants().getUserIds()) {
                if (!session.disconnectedUsersView().contains(uid)) {
                    winner = uid; break;
                }
            }
            if (winner != null) {
                for (String uid : ctx.participants().getUserIds()) {
                    if (uid.equals(winner)) ctx.outcomes().updateOutcome(uid, PlayerResult.WIN);
                    else ctx.outcomes().updateOutcome(uid, PlayerResult.LOSS);
                }
                // Cancel any active timers now that the game is decided by forfeit
                services.turnTimeoutScheduler().cancel(session.sessionId());
                services.decisionTimeoutScheduler().cancel(session.sessionId());
                // Allow outcome rules to adjust before finalization
                return new AbandonmentResult(
                    StateStep.transition(GameSessionStateType.POST_GAME_DECISION_WAITING.toStateName()),
                    true
                );
            }
        }
        return null;
    }

    private void fireRules(GameSessionStateContext context, TurnSnapshot snapshot) {
        RuleRuntimeContext runtime = new RuleRuntimeContext(
            services,
            contextService,
            context,
            snapshot,
            RuleTriggerKind.TURN_ROUND_COMPLETED
        );
        RuleService ruleService = RuleService.getInstance();
        ruleService.updateTurnBudget(context.rules(), runtime);
        ruleService.adjustTurnOrder(context.rules(), runtime);
        ruleService.activateRules(context.rules(), runtime);
    }

    private void evaluateOutcomeRules(GameSessionStateContext context) {
        if (context.outcomes().isGameFinished()) {
            return;
        }
        TurnSnapshot snapshot = turnContextService.peekTurnSnapshot(context);
        if (snapshot == null) {
            snapshot = services.turnService().snapshot(context.turns());
        }
        RuleRuntimeContext runtime = new RuleRuntimeContext(
            services,
            contextService,
            context,
            snapshot,
            RuleTriggerKind.OUTCOME_EVALUATION
        );
        RuleService ruleService = RuleService.getInstance();
        ruleService.activateRules(context.rules(), runtime);
        ruleService.applyOutcomeRules(context, runtime);
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

    private StateStep finalizeSession(GameSessionStateContext ctx) {
        ctx.session().lock().lock();
        try {
            int turnCount = ctx.turns().actionNumber();
            long now = System.currentTimeMillis();
            ctx.lifecycle().markGameFinished(now, turnCount);
        } finally {
            ctx.session().lock().unlock();
        }
        services.messenger().broadcastGameCompleted(ctx.session());
        contextService.postGame().queueGameCompletion(ctx, new GameCompletionNotice());
        applyScoreAdjustments(ctx);
        return StateStep.transition(GameSessionStateType.POST_GAME_DECISION_WAITING.toStateName());
    }

    private record AbandonmentResult(StateStep step, boolean requiresFinalize) {
    }
}
