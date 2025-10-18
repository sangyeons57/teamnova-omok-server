package teamnova.omok.glue.game.session.states.state;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import teamnova.omok.glue.game.session.model.PostGameDecision;
import teamnova.omok.glue.game.session.interfaces.GameTurnService;
import teamnova.omok.glue.game.session.model.GameSession;
import teamnova.omok.glue.game.session.states.event.DecisionTimeoutEvent;
import teamnova.omok.glue.game.session.states.event.MoveEvent;
import teamnova.omok.glue.game.session.states.event.PostGameDecisionEvent;
import teamnova.omok.glue.game.session.states.event.ReadyEvent;
import teamnova.omok.glue.game.session.states.event.TimeoutEvent;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContext;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateType;
import teamnova.omok.glue.service.dto.MoveResult;
import teamnova.omok.glue.service.dto.MoveStatus;
import teamnova.omok.glue.service.dto.PostGameDecisionPrompt;
import teamnova.omok.glue.service.dto.PostGameDecisionResult;
import teamnova.omok.glue.service.dto.PostGameDecisionStatus;
import teamnova.omok.glue.service.dto.PostGameDecisionUpdate;
import teamnova.omok.glue.service.dto.ReadyResult;
import teamnova.omok.glue.service.dto.TurnTimeoutResult;
import teamnova.omok.modules.state_machine.interfaces.BaseEvent;
import teamnova.omok.modules.state_machine.interfaces.BaseState;
import teamnova.omok.modules.state_machine.interfaces.StateContext;
import teamnova.omok.modules.state_machine.models.StateName;
import teamnova.omok.modules.state_machine.models.StateStep;

/**
 * Collects rematch/leave decisions from players after a game has finished.
 */
public final class PostGameDecisionWaitingState implements BaseState {
    private final GameTurnService turnService;

    public PostGameDecisionWaitingState(GameTurnService turnService) {
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
        GameSession session = context.session();
        session.resetPostGameDecisions();
        long now = System.currentTimeMillis();
        long deadline = now + GameSession.POST_GAME_DECISION_DURATION_MILLIS;
        context.postGameDecisionDeadline(deadline);
        context.pendingDecisionPrompt(new PostGameDecisionPrompt(session, deadline));
        context.pendingDecisionUpdate(snapshotUpdate(session));
        return StateStep.stay();
    }

    private StateStep onUpdateInternal(GameSessionStateContext context, long now) {
        if (allDecided(context.session())) {
            return StateStep.transition(GameSessionStateType.POST_GAME_DECISION_RESOLVING.toStateName());
        }
        return StateStep.stay();
    }

    private StateStep handleDecision(GameSessionStateContext context,
                                     PostGameDecisionEvent event) {
        GameSession session = context.session();
        if (!session.containsUser(event.userId())) {
            context.pendingDecisionResult(PostGameDecisionResult.rejected(
                session,
                event.userId(),
                PostGameDecisionStatus.INVALID_PLAYER
            ));
            return StateStep.stay();
        }
        long deadline = context.postGameDecisionDeadline();
        long now = System.currentTimeMillis();
        if (deadline > 0 && now > deadline) {
            context.pendingDecisionResult(PostGameDecisionResult.rejected(
                session,
                event.userId(),
                PostGameDecisionStatus.TIME_WINDOW_CLOSED
            ));
            return StateStep.stay();
        }
        if (session.hasPostGameDecision(event.userId())) {
            context.pendingDecisionResult(PostGameDecisionResult.rejected(
                session,
                event.userId(),
                PostGameDecisionStatus.ALREADY_DECIDED
            ));
            return StateStep.stay();
        }
        boolean recorded = session.recordPostGameDecision(event.userId(), event.decision());
        if (!recorded) {
            context.pendingDecisionResult(PostGameDecisionResult.rejected(
                session,
                event.userId(),
                PostGameDecisionStatus.ALREADY_DECIDED
            ));
            return StateStep.stay();
        }
        context.pendingDecisionResult(
            PostGameDecisionResult.accepted(session, event.userId(), event.decision())
        );
        context.pendingDecisionUpdate(snapshotUpdate(session));
        if (allDecided(session)) {
            return StateStep.transition(GameSessionStateType.POST_GAME_DECISION_RESOLVING.toStateName());
        }
        return StateStep.stay();
    }

    private StateStep handleDecisionTimeout(GameSessionStateContext context,
                                            DecisionTimeoutEvent event) {
        if (context.postGameDecisionDeadline() == 0L) {
            return StateStep.stay();
        }
        applyAutoLeaves(context.session());
        context.clearPostGameDecisionDeadline();
        context.pendingDecisionUpdate(snapshotUpdate(context.session()));
        return StateStep.transition(GameSessionStateType.POST_GAME_DECISION_RESOLVING.toStateName());
    }

    private StateStep handleReady(GameSessionStateContext context,
                                  ReadyEvent event) {
        GameSession session = context.session();
        if (!session.containsUser(event.userId())) {
            context.pendingReadyResult(ReadyResult.invalid(session, event.userId()));
            return StateStep.stay();
        }
        GameTurnService.TurnSnapshot snapshot =
            turnService.snapshot(session.getTurnStore(), session.getUserIds());
        context.pendingReadyResult(new ReadyResult(
            session,
            true,
            false,
            false,
            false,
            snapshot,
            event.userId()
        ));
        return StateStep.stay();
    }

    private StateStep handleMove(GameSessionStateContext context,
                                 MoveEvent event) {
        GameSession session = context.session();
        context.pendingMoveResult(MoveResult.invalid(
            session,
            MoveStatus.GAME_FINISHED,
            null,
            event.userId(),
            event.x(),
            event.y()
        ));
        return StateStep.stay();
    }

    private StateStep handleTurnTimeout(GameSessionStateContext context,
                                        TimeoutEvent event) {
        GameSession session = context.session();
        GameTurnService.TurnSnapshot snapshot =
            turnService.snapshot(session.getTurnStore(), session.getUserIds());
        context.pendingTimeoutResult(
            TurnTimeoutResult.noop(session, snapshot)
        );
        return StateStep.stay();
    }

    private boolean allDecided(GameSession session) {
        return session.postGameDecisionsView().size() >= session.getUserIds().size();
    }

    private void applyAutoLeaves(GameSession session) {
        for (String userId : session.getUserIds()) {
            if (!session.hasPostGameDecision(userId)) {
                session.recordPostGameDecision(userId, PostGameDecision.LEAVE);
            }
        }
    }

    private PostGameDecisionUpdate snapshotUpdate(GameSession session) {
        Map<String, PostGameDecision> decisions = session.postGameDecisionsView();
        List<String> remaining = new ArrayList<>();
        for (String userId : session.getUserIds()) {
            if (!decisions.containsKey(userId)) {
                remaining.add(userId);
            }
        }
        return new PostGameDecisionUpdate(session, decisions, remaining);
    }
}
