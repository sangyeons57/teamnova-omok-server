package teamnova.omok.domain.session.game.entity.state.state;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import teamnova.omok.game.PostGameDecisionType;
import teamnova.omok.service.dto.MoveResult;
import teamnova.omok.service.dto.MoveStatus;
import teamnova.omok.service.dto.PostGameDecisionPrompt;
import teamnova.omok.service.dto.PostGameDecisionResult;
import teamnova.omok.service.dto.PostGameDecisionStatus;
import teamnova.omok.service.dto.PostGameDecisionUpdate;
import teamnova.omok.service.dto.ReadyResult;
import teamnova.omok.service.dto.TurnTimeoutResult;
import teamnova.omok.domain.session.game.entity.turn.TurnSnapshot;
import teamnova.omok.domain.session.game.entity.state.contract.GameSessionEvent;
import teamnova.omok.domain.session.game.entity.state.contract.GameSessionState;
import teamnova.omok.domain.session.game.entity.state.manage.GameSessionStateContext;
import teamnova.omok.domain.session.game.entity.state.manage.GameSessionStateStep;
import teamnova.omok.domain.session.game.entity.state.manage.GameSessionStateType;
import teamnova.omok.domain.session.game.GameSession;

/**
 * Collects rematch/leave decisions from players after a game has finished.
 */
public final class PostGameDecisionWaitingState implements GameSessionState {
    @Override
    public GameSessionStateType type() {
        return GameSessionStateType.POST_GAME_DECISION_WAITING;
    }

    @Override
    public GameSessionStateStep onEvent(GameSessionStateContext context, GameSessionEvent event) {
        return switch (event) {
            case GameSessionEvent.Ready ignored ->  handleReady(context, (GameSessionEvent.Ready) event);
            case GameSessionEvent.Move ignored ->  handleMove(context, (GameSessionEvent.Move) event);
            case GameSessionEvent.Timeout ignored -> handleTurnTimeout(context, (GameSessionEvent.Timeout) event);
            case GameSessionEvent.PostGameDecision ignored -> handleDecision(context, (GameSessionEvent.PostGameDecision) event);
            case GameSessionEvent.DecisionTimeout ignored -> handleDecisionTimeout(context, (GameSessionEvent.DecisionTimeout) event);
            default -> GameSessionStateStep.stay();
        };
    }

    @Override
    public GameSessionStateStep onEnter(GameSessionStateContext context) {
        GameSession session = context.session();
        session.resetPostGameDecisions();
        long now = System.currentTimeMillis();
        long deadline = now + GameSession.POST_GAME_DECISION_DURATION_MILLIS;
        context.postGameDecisionDeadline(deadline);
        context.pendingDecisionPrompt(new PostGameDecisionPrompt(session, deadline));
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
                                                GameSessionEvent.PostGameDecision event) {
        GameSession session = context.session();
        if (!session.containsUser(event.userId())) {
            context.pendingDecisionResult(PostGameDecisionResult.rejected(
                session,
                event.userId(),
                PostGameDecisionStatus.INVALID_PLAYER
            ));
            return GameSessionStateStep.stay();
        }
        long deadline = context.postGameDecisionDeadline();
        long now = System.currentTimeMillis();
        if (deadline > 0 && now > deadline) {
            context.pendingDecisionResult(PostGameDecisionResult.rejected(
                session,
                event.userId(),
                PostGameDecisionStatus.TIME_WINDOW_CLOSED
            ));
            return GameSessionStateStep.stay();
        }
        if (session.hasPostGameDecision(event.userId())) {
            context.pendingDecisionResult(PostGameDecisionResult.rejected(
                session,
                event.userId(),
                PostGameDecisionStatus.ALREADY_DECIDED
            ));
            return GameSessionStateStep.stay();
        }
        boolean recorded = session.recordPostGameDecision(event.userId(), event.decision());
        if (!recorded) {
            context.pendingDecisionResult(PostGameDecisionResult.rejected(
                session,
                event.userId(),
                PostGameDecisionStatus.ALREADY_DECIDED
            ));
            return GameSessionStateStep.stay();
        }
        context.pendingDecisionResult(
            PostGameDecisionResult.accepted(session, event.userId(), event.decision())
        );
        context.pendingDecisionUpdate(snapshotUpdate(session));
        if (allDecided(session)) {
            return GameSessionStateStep.transition(GameSessionStateType.POST_GAME_DECISION_RESOLVING);
        }
        return GameSessionStateStep.stay();
    }

    private GameSessionStateStep handleDecisionTimeout(GameSessionStateContext context,
                                                       GameSessionEvent.DecisionTimeout event) {
        if (context.postGameDecisionDeadline() == 0L) {
            return GameSessionStateStep.stay();
        }
        applyAutoLeaves(context.session());
        context.clearPostGameDecisionDeadline();
        context.pendingDecisionUpdate(snapshotUpdate(context.session()));
        return GameSessionStateStep.transition(GameSessionStateType.POST_GAME_DECISION_RESOLVING);
    }

    private GameSessionStateStep handleReady(GameSessionStateContext context,
                                             GameSessionEvent.Ready event) {
        GameSession session = context.session();
        if (!session.containsUser(event.userId())) {
            context.pendingReadyResult(ReadyResult.invalid(session, event.userId()));
            return GameSessionStateStep.stay();
        }
        TurnSnapshot snapshot = session.snapshotTurn();
        context.pendingReadyResult(new ReadyResult(
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
                                            GameSessionEvent.Move event) {
        GameSession session = context.session();
        context.pendingMoveResult(MoveResult.invalid(
            session,
            MoveStatus.GAME_FINISHED,
            null,
            event.userId(),
            event.x(),
            event.y()
        ));
        return GameSessionStateStep.stay();
    }

    private GameSessionStateStep handleTurnTimeout(GameSessionStateContext context,
                                                   GameSessionEvent.Timeout event) {
        GameSession session = context.session();
        TurnSnapshot snapshot = session.snapshotTurn();
        context.pendingTimeoutResult(
            TurnTimeoutResult.noop(session, snapshot)
        );
        return GameSessionStateStep.stay();
    }

    private boolean allDecided(GameSession session) {
        return session.postGameDecisionsView().size() >= session.getUserIds().size();
    }

    private void applyAutoLeaves(GameSession session) {
        for (String userId : session.getUserIds()) {
            if (!session.hasPostGameDecision(userId)) {
                session.recordPostGameDecision(userId, PostGameDecisionType.LEAVE);
            }
        }
    }

    private PostGameDecisionUpdate snapshotUpdate(GameSession session) {
        Map<String, PostGameDecisionType> decisions = session.postGameDecisionsView();
        List<String> remaining = new ArrayList<>();
        for (String userId : session.getUserIds()) {
            if (!decisions.containsKey(userId)) {
                remaining.add(userId);
            }
        }
        return new PostGameDecisionUpdate(session, decisions, remaining);
    }
}
