package teamnova.omok.state.game.state;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import teamnova.omok.game.PostGameDecision;
import teamnova.omok.service.InGameSessionService;
import teamnova.omok.service.TurnService;
import teamnova.omok.state.game.contract.GameSessionState;
import teamnova.omok.state.game.event.DecisionTimeoutEvent;
import teamnova.omok.state.game.event.GameSessionEventRegistry;
import teamnova.omok.state.game.event.GameSessionEventType;
import teamnova.omok.state.game.event.MoveEvent;
import teamnova.omok.state.game.event.PostGameDecisionEvent;
import teamnova.omok.state.game.event.ReadyEvent;
import teamnova.omok.state.game.event.TimeoutEvent;
import teamnova.omok.state.game.manage.GameSessionStateContext;
import teamnova.omok.state.game.manage.GameSessionStateStep;
import teamnova.omok.state.game.manage.GameSessionStateType;
import teamnova.omok.store.GameSession;

/**
 * Collects rematch/leave decisions from players after a game has finished.
 */
public final class PostGameDecisionWaitingState implements GameSessionState {
    @Override
    public GameSessionStateType type() {
        return GameSessionStateType.POST_GAME_DECISION_WAITING;
    }

    @Override
    public void registerHandlers(GameSessionEventRegistry registry) {
        registry.register(GameSessionEventType.POST_GAME_DECISION, PostGameDecisionEvent.class, this::handleDecision);
        registry.register(GameSessionEventType.DECISION_TIMEOUT, DecisionTimeoutEvent.class, this::handleDecisionTimeout);
        registry.register(GameSessionEventType.READY, ReadyEvent.class, this::handleReady);
        registry.register(GameSessionEventType.MOVE, MoveEvent.class, this::handleMove);
        registry.register(GameSessionEventType.TIMEOUT, TimeoutEvent.class, this::handleTurnTimeout);
    }

    @Override
    public GameSessionStateStep onEnter(GameSessionStateContext context) {
        GameSession session = context.session();
        session.resetPostGameDecisions();
        long now = System.currentTimeMillis();
        long deadline = now + GameSession.POST_GAME_DECISION_DURATION_MILLIS;
        context.postGameDecisionDeadline(deadline);
        context.pendingDecisionPrompt(new InGameSessionService.PostGameDecisionPrompt(session, deadline));
        context.pendingDecisionUpdate(snapshotUpdate(session));
        return GameSessionStateStep.stay();
    }

    @Override
    public GameSessionStateStep onUpdate(GameSessionStateContext context, long now) {
        if (allDecided(context.session())) {
            return GameSessionStateStep.transition(GameSessionStateType.POST_GAME_DECISION_RESOLVING);
        }
        return GameSessionStateStep.stay();
    }

    private GameSessionStateStep handleDecision(GameSessionStateContext context,
                                                PostGameDecisionEvent event) {
        GameSession session = context.session();
        if (!session.containsUser(event.userId())) {
            context.pendingDecisionResult(InGameSessionService.PostGameDecisionResult.rejected(
                session,
                event.userId(),
                InGameSessionService.PostGameDecisionStatus.INVALID_PLAYER
            ));
            return GameSessionStateStep.stay();
        }
        long deadline = context.postGameDecisionDeadline();
        long now = System.currentTimeMillis();
        if (deadline > 0 && now > deadline) {
            context.pendingDecisionResult(InGameSessionService.PostGameDecisionResult.rejected(
                session,
                event.userId(),
                InGameSessionService.PostGameDecisionStatus.TIME_WINDOW_CLOSED
            ));
            return GameSessionStateStep.stay();
        }
        if (session.hasPostGameDecision(event.userId())) {
            context.pendingDecisionResult(InGameSessionService.PostGameDecisionResult.rejected(
                session,
                event.userId(),
                InGameSessionService.PostGameDecisionStatus.ALREADY_DECIDED
            ));
            return GameSessionStateStep.stay();
        }
        boolean recorded = session.recordPostGameDecision(event.userId(), event.decision());
        if (!recorded) {
            context.pendingDecisionResult(InGameSessionService.PostGameDecisionResult.rejected(
                session,
                event.userId(),
                InGameSessionService.PostGameDecisionStatus.ALREADY_DECIDED
            ));
            return GameSessionStateStep.stay();
        }
        context.pendingDecisionResult(
            InGameSessionService.PostGameDecisionResult.accepted(session, event.userId(), event.decision())
        );
        context.pendingDecisionUpdate(snapshotUpdate(session));
        if (allDecided(session)) {
            return GameSessionStateStep.transition(GameSessionStateType.POST_GAME_DECISION_RESOLVING);
        }
        return GameSessionStateStep.stay();
    }

    private GameSessionStateStep handleDecisionTimeout(GameSessionStateContext context,
                                                       DecisionTimeoutEvent event) {
        if (context.postGameDecisionDeadline() == 0L) {
            return GameSessionStateStep.stay();
        }
        applyAutoLeaves(context.session());
        context.clearPostGameDecisionDeadline();
        context.pendingDecisionUpdate(snapshotUpdate(context.session()));
        return GameSessionStateStep.transition(GameSessionStateType.POST_GAME_DECISION_RESOLVING);
    }

    private GameSessionStateStep handleReady(GameSessionStateContext context,
                                             ReadyEvent event) {
        GameSession session = context.session();
        if (!session.containsUser(event.userId())) {
            context.pendingReadyResult(InGameSessionService.ReadyResult.invalid(session, event.userId()));
            return GameSessionStateStep.stay();
        }
        TurnService.TurnSnapshot snapshot =
            context.turnService().snapshot(session.getTurnStore(), session.getUserIds());
        context.pendingReadyResult(new InGameSessionService.ReadyResult(
            session,
            true,
            false,
            false,
            false,
            snapshot,
            event.userId()
        ));
        return GameSessionStateStep.stay();
    }

    private GameSessionStateStep handleMove(GameSessionStateContext context,
                                            MoveEvent event) {
        GameSession session = context.session();
        context.pendingMoveResult(InGameSessionService.MoveResult.invalid(
            session,
            InGameSessionService.MoveStatus.GAME_FINISHED,
            null,
            event.userId(),
            event.x(),
            event.y()
        ));
        return GameSessionStateStep.stay();
    }

    private GameSessionStateStep handleTurnTimeout(GameSessionStateContext context,
                                                   TimeoutEvent event) {
        GameSession session = context.session();
        TurnService.TurnSnapshot snapshot =
            context.turnService().snapshot(session.getTurnStore(), session.getUserIds());
        context.pendingTimeoutResult(
            InGameSessionService.TurnTimeoutResult.noop(session, snapshot)
        );
        return GameSessionStateStep.stay();
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

    private InGameSessionService.PostGameDecisionUpdate snapshotUpdate(GameSession session) {
        Map<String, PostGameDecision> decisions = session.postGameDecisionsView();
        List<String> remaining = new ArrayList<>();
        for (String userId : session.getUserIds()) {
            if (!decisions.containsKey(userId)) {
                remaining.add(userId);
            }
        }
        return new InGameSessionService.PostGameDecisionUpdate(session, decisions, remaining);
    }
}
