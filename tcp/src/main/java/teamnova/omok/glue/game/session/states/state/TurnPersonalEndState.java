package teamnova.omok.glue.game.session.states.state;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import teamnova.omok.glue.game.session.interfaces.GameTurnService;
import teamnova.omok.glue.game.session.log.GameSessionLogger;
import teamnova.omok.glue.game.session.model.PlayerResult;
import teamnova.omok.glue.game.session.model.Stone;
import teamnova.omok.glue.game.session.model.dto.GameSessionServices;
import teamnova.omok.glue.game.session.model.dto.TurnSnapshot;
import teamnova.omok.glue.game.session.model.result.MoveStatus;
import teamnova.omok.glue.game.session.model.runtime.TurnPersonalFrame;
import teamnova.omok.glue.game.session.services.RuleService;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContext;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContextService;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateType;
import teamnova.omok.glue.game.session.states.manage.GameSessionTurnContextService;
import teamnova.omok.glue.rule.runtime.RuleRuntimeContext;
import teamnova.omok.glue.rule.api.RuleTriggerKind;
import teamnova.omok.modules.state_machine.interfaces.BaseState;
import teamnova.omok.modules.state_machine.interfaces.StateContext;
import teamnova.omok.modules.state_machine.models.StateName;
import teamnova.omok.modules.state_machine.models.StateStep;

/**
 * Resolves the end of a personal turn, whether it finished through a move or timeout.
 */
public final class TurnPersonalEndState implements BaseState {
    private final GameSessionStateContextService contextService;
    private final GameSessionTurnContextService turnContextService;
    private final GameSessionServices services;
    private final GameTurnService turnService;

    public TurnPersonalEndState(GameSessionStateContextService contextService,
                                GameSessionServices services) {
        this.contextService = Objects.requireNonNull(contextService, "contextService");
        this.turnContextService = contextService.turn();
        this.services = Objects.requireNonNull(services, "services");
        this.turnService = Objects.requireNonNull(services.turnService(), "turnService");
    }

    @Override
    public StateName name() {
        return GameSessionStateType.TURN_PERSONAL_END.toStateName();
    }

    @Override
    public <I extends StateContext> StateStep onEnter(I context) {
        return onEnterInternal((GameSessionStateContext) context);
    }

    private StateStep onEnterInternal(GameSessionStateContext context) {
        TurnPersonalFrame frame = turnContextService.currentPersonalTurn(context);
        if (frame == null) {
            return StateStep.transition(GameSessionStateType.TURN_WAITING.toStateName());
        }
        if (frame.hasActiveMove()) {
            return resolveMoveCompletion(context, frame);
        }
        if (frame.hasTimeoutOutcome()) {
            return resolveTimeoutCompletion(context, frame);
        }
        return StateStep.transition(GameSessionStateType.TURN_WAITING.toStateName());
    }

    private StateStep resolveMoveCompletion(GameSessionStateContext context,
                                            TurnPersonalFrame frame) {
        boolean finished = handleStonePlaced(context, frame.userId(), frame.x(), frame.y(), frame.stone());
        if (finished) {
            GameSessionLogger.event(context, GameSessionStateType.TURN_PERSONAL_END, "WinDetected",
                String.format("winner=%s stone=%s", frame.userId(), frame.stone()));
            completeWinningTurn(context, frame);
            emitTurnEnded(context, frame);
            return StateStep.transition(GameSessionStateType.TURN_END.toStateName());
        }

        if (evaluateOutcomeRulesNow(context, "post-move")) {
            turnContextService.finalizeMoveOutcome(context, MoveStatus.SUCCESS);
            turnContextService.clearTurnCycle(context);
            emitTurnEnded(context, frame);
            return StateStep.transition(GameSessionStateType.TURN_END.toStateName());
        }

        TurnSnapshot nextSnapshot = turnService.advanceSkippingDisconnected(
            context.turns(),
            context.participants().disconnectedUsersView(),
            frame.requestedAtMillis()
        );
        frame.nextSnapshot(nextSnapshot);
        if (nextSnapshot != null) {
            turnContextService.recordTurnSnapshot(context, nextSnapshot, frame.requestedAtMillis());
            fireRules(context, RuleTriggerKind.TURN_ADVANCE, nextSnapshot);
        }
        turnContextService.finalizeMoveOutcome(context, MoveStatus.SUCCESS);
        turnContextService.clearTurnCycle(context);
        GameSessionLogger.event(context, GameSessionStateType.TURN_PERSONAL_END, "TurnFinalized",
            String.format("user=%s nextPlayer=%s wrapped=%s",
                frame.userId(),
                nextSnapshot != null ? nextSnapshot.currentPlayerId() : "-",
                nextSnapshot != null && nextSnapshot.wrapped()));
        emitTurnEnded(context, frame);
        return decideNextState(nextSnapshot);
    }

    private StateStep resolveTimeoutCompletion(GameSessionStateContext context,
                                               TurnPersonalFrame frame) {
        long occurredAt = frame.timeoutOccurredAtMillis() > 0
            ? frame.timeoutOccurredAtMillis()
            : System.currentTimeMillis();
        TurnSnapshot nextSnapshot = turnService.advanceSkippingDisconnected(
            context.turns(),
            context.participants().disconnectedUsersView(),
            occurredAt
        );
        frame.nextSnapshot(nextSnapshot);
        frame.resolveTimeout(frame.timeoutTimedOut(),
            nextSnapshot,
            occurredAt);
        if (nextSnapshot != null) {
            turnContextService.recordTurnSnapshot(context, nextSnapshot, occurredAt);
            fireRules(context, RuleTriggerKind.TURN_ADVANCE, nextSnapshot);
        }
        turnContextService.clearTurnCycle(context);
        if (evaluateOutcomeRulesNow(context, "timeout")) {
            emitTurnEnded(context, frame);
            return StateStep.transition(GameSessionStateType.TURN_END.toStateName());
        }
        emitTurnEnded(context, frame);
        return decideNextState(nextSnapshot);
    }

    private StateStep decideNextState(TurnSnapshot nextSnapshot) {
        if (nextSnapshot == null || nextSnapshot.wrapped()) {
            return StateStep.transition(GameSessionStateType.TURN_END.toStateName());
        }
        return StateStep.transition(GameSessionStateType.TURN_PERSONAL_START.toStateName());
    }

    private void completeWinningTurn(GameSessionStateContext context, TurnPersonalFrame frame) {
        // Only finalize the personal move outcome here; do not mark the whole game as finished.
        // TurnEndState will be the sole authority to mark game completion and transition to Post-Game.
        turnContextService.finalizeMoveOutcome(context, MoveStatus.SUCCESS);
        turnContextService.clearTurnCycle(context);
    }

    private void emitTurnEnded(GameSessionStateContext context, TurnPersonalFrame frame) {
        services.messenger().broadcastTurnEnded(context.session(), frame);
    }

    private boolean evaluateOutcomeRulesNow(GameSessionStateContext context, String detail) {
        if (context.outcomes().isGameFinished()) {
            return true;
        }
        TurnSnapshot snapshot = services.turnService().snapshot(context.turns());
        RuleRuntimeContext runtime = new RuleRuntimeContext(
            services,
            contextService,
            context,
            snapshot,
            RuleTriggerKind.OUTCOME_EVALUATION
        );
        RuleService ruleService = RuleService.getInstance();
        ruleService.activateRules(context.rules(), runtime);
        var resolution = ruleService.applyOutcomeRules(context, runtime);
        resolution.ifPresent(res -> {
            if (res.finalizeNow()) {
                GameSessionLogger.event(context, GameSessionStateType.TURN_PERSONAL_END, "OutcomeRuleFinalize",
                    detail);
            }
        });
        return context.outcomes().isGameFinished();
    }

    private void fireRules(GameSessionStateContext context,
                           RuleTriggerKind trigger,
                           TurnSnapshot snapshot) {
        if (snapshot == null) {
            return;
        }
        RuleRuntimeContext runtime = new RuleRuntimeContext(
            services,
            contextService,
            context,
            snapshot,
            trigger
        );
        RuleService ruleService = RuleService.getInstance();
        if (trigger == RuleTriggerKind.TURN_ADVANCE) {
            ruleService.updateTurnBudget(context.rules(), runtime);
            ruleService.adjustTurnTiming(context.rules(), runtime);
        }
        if (trigger == RuleTriggerKind.TURN_ROUND_COMPLETED) {
            ruleService.updateTurnBudget(context.rules(), runtime);
        }
        ruleService.activateRules(context.rules(), runtime);
    }

    private boolean handleStonePlaced(GameSessionStateContext context,
                                      String userId,
                                      int x,
                                      int y,
                                      Stone stone) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(stone, "stone");
        if (context.outcomes().isGameFinished()) {
            return true;
        }
        if (!services.boardService().hasFiveInARow(context.board(), x, y, stone)) {
            return false;
        }
        for (String disconnectedId : context.participants().disconnectedUsersView()) {
            if (!disconnectedId.equals(userId)) {
                context.outcomes().updateOutcome(disconnectedId, PlayerResult.LOSS);
            }
        }
        List<String> userIds = context.participants().getUserIds();
        for (String uid : userIds) {
            if (uid.equals(userId)) {
                context.outcomes().updateOutcome(uid, PlayerResult.WIN);
            } else {
                context.outcomes().updateOutcome(uid, PlayerResult.LOSS);
            }
        }
        System.out.printf(
            "[OutcomeService] Game %s finished: winner=%s stone=%s position=(%d,%d)%n",
            context.session().sessionId().asUuid(),
            userId,
            stone,
            x,
            y
        );
        return true;
    }
}
