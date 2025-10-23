package teamnova.omok.glue.game.session.states.state;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import teamnova.omok.glue.game.session.interfaces.GameTurnService;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionAccess;
import teamnova.omok.glue.game.session.model.GameSession;
import teamnova.omok.glue.game.session.model.PostGameDecision;
import teamnova.omok.glue.game.session.model.dto.TurnSnapshot;
import teamnova.omok.glue.game.session.states.event.DecisionTimeoutEvent;
import teamnova.omok.glue.game.session.states.event.MoveEvent;
import teamnova.omok.glue.game.session.states.event.PostGameDecisionEvent;
import teamnova.omok.glue.game.session.states.event.ReadyEvent;
import teamnova.omok.glue.game.session.states.event.TimeoutEvent;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContext;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContextService;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateType;
import teamnova.omok.glue.game.session.model.messages.PostGameDecisionPrompt;
import teamnova.omok.glue.game.session.model.result.PostGameDecisionResult;
import teamnova.omok.glue.game.session.model.result.PostGameDecisionStatus;
import teamnova.omok.glue.game.session.model.messages.PostGameDecisionUpdate;
import teamnova.omok.glue.game.session.model.result.ReadyResult;
import teamnova.omok.modules.state_machine.interfaces.BaseEvent;
import teamnova.omok.modules.state_machine.interfaces.BaseState;
import teamnova.omok.modules.state_machine.interfaces.StateContext;
import teamnova.omok.modules.state_machine.models.StateName;
import teamnova.omok.modules.state_machine.models.StateStep;

/**
 * Collects rematch/leave decisions from players after a game has finished.
 */
public final class PostGameDecisionWaitingState implements BaseState {
    private final GameSessionStateContextService contextService;
    private final GameTurnService turnService;

    public PostGameDecisionWaitingState(GameSessionStateContextService contextService,
                                        GameTurnService turnService) {
        this.contextService = Objects.requireNonNull(contextService, "contextService");
        this.turnService = Objects.requireNonNull(turnService, "turnService");
    }
    @Override
    public StateName name() {
        return GameSessionStateType.POST_GAME_DECISION_WAITING.toStateName();
    }

    @Override
    public <I extends StateContext> StateStep onEnter(I context) {
        return onEnterInternal((GameSessionStateContext) context);
    }

    @Override
    public <I extends StateContext> StateStep onUpdate(I context, long now) {
        return onUpdateInternal((GameSessionStateContext) context, now);
    }

    @Override
    public <I extends StateContext> StateStep onEvent(I context, BaseEvent event) {
        GameSessionStateContext gameContext = (GameSessionStateContext) context;
        if (event instanceof PostGameDecisionEvent decisionEvent) {
            return handleDecision(gameContext, decisionEvent);
        }
        if (event instanceof DecisionTimeoutEvent decisionTimeoutEvent) {
            return handleDecisionTimeout(gameContext, decisionTimeoutEvent);
        }
        if (event instanceof ReadyEvent readyEvent) {
            return handleReady(gameContext, readyEvent);
        }
        if (event instanceof MoveEvent moveEvent) {
            return handleMove(gameContext, moveEvent);
        }
        if (event instanceof TimeoutEvent timeoutEvent) {
            return handleTurnTimeout(gameContext, timeoutEvent);
        }
        return StateStep.stay();
    }

    private StateStep onEnterInternal(GameSessionStateContext context) {
        context.postGame().resetPostGameDecisions();
        long now = System.currentTimeMillis();
        long deadline = now + GameSession.POST_GAME_DECISION_DURATION_MILLIS;
        contextService.postGame().setDecisionDeadline(context, deadline);
        contextService.postGame().queueDecisionPrompt(context, new PostGameDecisionPrompt(deadline));
        contextService.postGame().queueDecisionUpdate(context, snapshotUpdate(context));
        return StateStep.stay();
    }

    private StateStep onUpdateInternal(GameSessionStateContext context, long now) {
        if (allDecided(context)) {
            return StateStep.transition(GameSessionStateType.POST_GAME_DECISION_RESOLVING.toStateName());
        }
        return StateStep.stay();
    }

    private StateStep handleDecision(GameSessionStateContext context,
                                     PostGameDecisionEvent event) {
        if (!context.participants().containsUser(event.userId())) {
            contextService.postGame().queueDecisionResult(context, PostGameDecisionResult.rejected(
                event.userId(),
                PostGameDecisionStatus.INVALID_PLAYER
            ));
            return StateStep.stay();
        }
        long deadline = contextService.postGame().decisionDeadline(context);
        long now = System.currentTimeMillis();
        if (deadline > 0 && now > deadline) {
            contextService.postGame().queueDecisionResult(context, PostGameDecisionResult.rejected(
                event.userId(),
                PostGameDecisionStatus.TIME_WINDOW_CLOSED
            ));
            return StateStep.stay();
        }
        if (context.postGame().hasPostGameDecision(event.userId())) {
            contextService.postGame().queueDecisionResult(context, PostGameDecisionResult.rejected(
                event.userId(),
                PostGameDecisionStatus.ALREADY_DECIDED
            ));
            return StateStep.stay();
        }
        boolean recorded = context.postGame().recordPostGameDecision(event.userId(), event.decision());
        if (!recorded) {
            contextService.postGame().queueDecisionResult(context, PostGameDecisionResult.rejected(
                event.userId(),
                PostGameDecisionStatus.ALREADY_DECIDED
            ));
            return StateStep.stay();
        }
        // If the player chose to LEAVE, mark them disconnected immediately
        if (event.decision() == PostGameDecision.LEAVE) {
            var session = context.session();
            session.lock().lock();
            try {
                session.markDisconnected(event.userId());
            } finally {
                session.lock().unlock();
            }
        }
        contextService.postGame().queueDecisionResult(context,
            PostGameDecisionResult.accepted(event.userId(), event.decision())
        );
        contextService.postGame().queueDecisionUpdate(context, snapshotUpdate(context));
        if (allDecided(context)) {
            return StateStep.transition(GameSessionStateType.POST_GAME_DECISION_RESOLVING.toStateName());
        }
        return StateStep.stay();
    }

    private StateStep handleDecisionTimeout(GameSessionStateContext context,
                                            DecisionTimeoutEvent event) {
        if (contextService.postGame().decisionDeadline(context) == 0L) {
            return StateStep.stay();
        }
        applyAutoLeaves(context);
        contextService.postGame().clearDecisionDeadline(context);
        contextService.postGame().queueDecisionUpdate(context, snapshotUpdate(context));
        return StateStep.transition(GameSessionStateType.POST_GAME_DECISION_RESOLVING.toStateName());
    }

    private StateStep handleReady(GameSessionStateContext context,
                                  ReadyEvent event) {
        GameSessionAccess session = context.session();
        if (!context.participants().containsUser(event.userId())) {
            contextService.turn().queueReadyResult(context, ReadyResult.invalid(event.userId(), event.requestId()));
            return StateStep.stay();
        }
        TurnSnapshot snapshot =
            turnService.snapshot(context.turns());
        contextService.turn().queueReadyResult(context, new ReadyResult(
            true,
            false,
            false,
            false,
            snapshot,
            event.userId(),
            event.requestId()
        ));
        return StateStep.stay();
    }

    private StateStep handleMove(GameSessionStateContext context,
                                 MoveEvent event) {
        return StateStep.stay();
    }

    private StateStep handleTurnTimeout(GameSessionStateContext context,
                                        TimeoutEvent event) {
        return StateStep.stay();
    }

    private boolean allDecided(GameSessionStateContext context) {
        return context.postGame().postGameDecisionsView().size() >= context.participants().getUserIds().size();
    }

    private void applyAutoLeaves(GameSessionStateContext context) {
        for (String userId : context.participants().getUserIds()) {
            if (!context.postGame().hasPostGameDecision(userId)) {
                context.postGame().recordPostGameDecision(userId, PostGameDecision.LEAVE);
                // Mark undecided users as disconnected on timeout expiry
                var session = context.session();
                session.lock().lock();
                try {
                    session.markDisconnected(userId);
                } finally {
                    session.lock().unlock();
                }
            }
        }
    }

    private PostGameDecisionUpdate snapshotUpdate(GameSessionStateContext context) {
        Map<String, PostGameDecision> decisions = context.postGame().postGameDecisionsView();
        List<String> remaining = new ArrayList<>();
        for (String userId : context.participants().getUserIds()) {
            if (!decisions.containsKey(userId)) {
                remaining.add(userId);
            }
        }
        return new PostGameDecisionUpdate(decisions, remaining);
    }
}
