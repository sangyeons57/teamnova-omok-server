package teamnova.omok.glue.game.session.services;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import teamnova.omok.glue.game.session.interfaces.GameTurnService;
import teamnova.omok.glue.game.session.interfaces.TurnTimeoutScheduler;
import teamnova.omok.glue.game.session.model.GameSession;
import teamnova.omok.glue.game.session.model.PostGameDecision;
import teamnova.omok.glue.game.session.model.dto.SessionSubmission;
import teamnova.omok.glue.game.session.model.vo.GameSessionId;
import teamnova.omok.glue.game.session.states.GameStateHub;
import teamnova.omok.glue.game.session.states.event.DecisionTimeoutEvent;
import teamnova.omok.glue.game.session.states.event.MoveEvent;
import teamnova.omok.glue.game.session.states.event.PostGameDecisionEvent;
import teamnova.omok.glue.game.session.states.event.ReadyEvent;
import teamnova.omok.glue.game.session.states.event.TimeoutEvent;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContext;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateType;
import teamnova.omok.glue.handler.register.Type;
import teamnova.omok.glue.game.session.model.messages.BoardSnapshotUpdate;
import teamnova.omok.glue.game.session.model.messages.GameCompletionNotice;
import teamnova.omok.glue.game.session.model.result.MoveResult;
import teamnova.omok.glue.game.session.model.result.MoveStatus;
import teamnova.omok.glue.game.session.model.messages.PostGameDecisionPrompt;
import teamnova.omok.glue.game.session.model.result.PostGameDecisionResult;
import teamnova.omok.glue.game.session.model.result.PostGameDecisionStatus;
import teamnova.omok.glue.game.session.model.messages.PostGameDecisionUpdate;
import teamnova.omok.glue.game.session.model.messages.PostGameResolution;
import teamnova.omok.glue.game.session.model.result.ReadyResult;
import teamnova.omok.glue.game.session.model.result.TurnTimeoutResult;

/**
 * Stateless utilities for submitting session events and coordinating side effects.
 */
public final class SessionEventService {

    private SessionEventService() {
    }

    public static boolean submitReady(GameSessionDependencies deps,
                                      TurnTimeoutScheduler.TurnTimeoutConsumer timeoutConsumer,
                                      String userId,
                                      long requestId) {
        Objects.requireNonNull(deps, "deps");
        Objects.requireNonNull(timeoutConsumer, "timeoutConsumer");
        Objects.requireNonNull(userId, "userId");
        return withSession(deps, userId, submission -> {
            ReadyEvent event = new ReadyEvent(userId, submission.timestamp(), requestId);
            submission.manager().submit(event,
                ctx -> handleReadyCompletion(deps, timeoutConsumer, submission.manager(), ctx, event));
        });
    }

    public static boolean submitMove(GameSessionDependencies deps,
                                     TurnTimeoutScheduler.TurnTimeoutConsumer timeoutConsumer,
                                     String userId,
                                     long requestId,
                                     int x,
                                     int y) {
        Objects.requireNonNull(deps, "deps");
        Objects.requireNonNull(timeoutConsumer, "timeoutConsumer");
        Objects.requireNonNull(userId, "userId");
        return withSession(deps, userId, submission -> {
            MoveEvent event = new MoveEvent(userId, x, y, submission.timestamp(), requestId);
            submission.manager().submit(event,
                ctx -> handleMoveCompletion(deps, timeoutConsumer, submission.manager(), ctx, event));
        });
    }

    public static boolean submitPostGameDecision(GameSessionDependencies deps,
                                                 TurnTimeoutScheduler.TurnTimeoutConsumer timeoutConsumer,
                                                 String userId,
                                                 long requestId,
                                                 PostGameDecision decision) {
        Objects.requireNonNull(deps, "deps");
        Objects.requireNonNull(timeoutConsumer, "timeoutConsumer");
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(decision, "decision");
        return withSession(deps, userId, submission -> {
            PostGameDecisionEvent event = new PostGameDecisionEvent(userId, decision, submission.timestamp(), requestId);
            submission.manager().submit(event,
                ctx -> handlePostGameDecisionCompletion(deps, timeoutConsumer, submission.manager(), ctx, event));
        });
    }

    public static void cancelAllTimers(GameSessionDependencies deps, GameSessionId sessionId) {
        Objects.requireNonNull(deps, "deps");
        Objects.requireNonNull(sessionId, "sessionId");
        deps.turnTimeoutScheduler().cancel(sessionId);
        deps.decisionTimeoutScheduler().cancel(sessionId);
    }

    public static void skipTurnForDisconnected(GameSessionDependencies deps,
                                               TurnTimeoutScheduler.TurnTimeoutConsumer timeoutConsumer,
                                               GameSession session,
                                               String userId,
                                               int expectedTurnNumber) {
        Objects.requireNonNull(deps, "deps");
        Objects.requireNonNull(timeoutConsumer, "timeoutConsumer");
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(userId, "userId");
        boolean shouldSubmit = false;
        session.lock().lock();
        try {
            if (session.isGameStarted() && !session.isGameFinished()) {
                GameTurnService.TurnSnapshot current =
                    deps.turnService().snapshot(session.getTurnStore());
                shouldSubmit = current != null
                    && current.turnNumber() == expectedTurnNumber
                    && userId.equals(current.currentPlayerId());
            }
        } finally {
            session.lock().unlock();
        }
        if (!shouldSubmit) {
            return;
        }
        deps.turnTimeoutScheduler().cancel(session.sessionId());
        GameStateHub manager = deps.runtime().ensure(session);
        long now = System.currentTimeMillis();
        TimeoutEvent event = new TimeoutEvent(expectedTurnNumber, now);
        manager.submit(event, ctx -> handleTimeoutCompletion(deps, timeoutConsumer, session, manager, ctx, event));
    }

    public static void handleScheduledTimeout(GameSessionDependencies deps,
                                              TurnTimeoutScheduler.TurnTimeoutConsumer timeoutConsumer,
                                              GameSessionId sessionId,
                                              int expectedTurnNumber) {
        Objects.requireNonNull(deps, "deps");
        Objects.requireNonNull(timeoutConsumer, "timeoutConsumer");
        Objects.requireNonNull(sessionId, "sessionId");
        if (!deps.turnTimeoutScheduler().validate(sessionId, expectedTurnNumber)) {
            return;
        }
        Optional<GameSession> optionalSession = deps.repository().findById(sessionId);
        if (optionalSession.isEmpty()) {
            return;
        }
        GameSession session = optionalSession.get();
        GameStateHub manager = deps.runtime().ensure(session);
        deps.turnTimeoutScheduler().clearIfMatches(sessionId, expectedTurnNumber);
        long now = System.currentTimeMillis();
        TimeoutEvent event = new TimeoutEvent(expectedTurnNumber, now);
        manager.submit(event, ctx -> handleTimeoutCompletion(deps, timeoutConsumer, session, manager, ctx, event));
    }

    private static boolean withSession(GameSessionDependencies deps,
                                       String userId,
                                       Consumer<SessionSubmission> consumer) {
        Objects.requireNonNull(deps, "deps");
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(consumer, "consumer");
        Optional<GameSession> optionalSession = deps.repository().findByUserId(userId);
        if (optionalSession.isEmpty()) {
            return false;
        }
        GameSession session = optionalSession.get();
        GameStateHub manager = deps.runtime().ensure(session);
        long now = System.currentTimeMillis();
        consumer.accept(new SessionSubmission(session, manager, now));
        return true;
    }

    private static void handleReadyCompletion(GameSessionDependencies deps,
                                              TurnTimeoutScheduler.TurnTimeoutConsumer timeoutConsumer,
                                              GameStateHub manager,
                                              GameSessionStateContext ctx,
                                              ReadyEvent event) {
        ReadyResult result = ctx.consumePendingReadyResult();
        if (result == null) {
            String message;
            GameSession session = manager.session();
            if (!session.containsUser(event.userId())) {
                message = "INVALID_PLAYER";
            } else if (manager.currentType() == GameSessionStateType.COMPLETED) {
                message = "GAME_FINISHED";
            } else if (!session.isGameStarted()) {
                message = "GAME_NOT_STARTED";
            } else {
                message = "INVALID_STATE";
            }
            deps.messenger().respondError(event.userId(), Type.READY_IN_GAME_SESSION, event.requestId(), message);
            drainPostGameSideEffects(deps, timeoutConsumer, ctx);
            return;
        }
        if (!result.validUser()) {
            deps.messenger().respondError(event.userId(), Type.READY_IN_GAME_SESSION, event.requestId(), "INVALID_PLAYER");
            drainPostGameSideEffects(deps, timeoutConsumer, ctx);
            return;
        }
        deps.messenger().respondReady(event.userId(), event.requestId(), result);
        if (result.stateChanged()) {
            deps.messenger().broadcastReady(result);
        }
        if (result.gameStartedNow() && result.firstTurn() != null) {
            deps.messenger().broadcastGameStart(result.session(), result.firstTurn());
            scheduleTurnTimeout(deps, timeoutConsumer, result.session(), result.firstTurn());
        }
        if (manager.currentType() == GameSessionStateType.COMPLETED) {
            cancelAllTimers(deps, result.session().sessionId());
        }
        drainPostGameSideEffects(deps, timeoutConsumer, ctx);
    }

    private static void handleMoveCompletion(GameSessionDependencies deps,
                                             TurnTimeoutScheduler.TurnTimeoutConsumer timeoutConsumer,
                                             GameStateHub manager,
                                             GameSessionStateContext ctx,
                                             MoveEvent event) {
        MoveResult result = ctx.consumePendingMoveResult();
        if (result == null) {
            GameSession session = manager.session();
            String message;
            if (!session.containsUser(event.userId())) {
                message = "INVALID_PLAYER";
            } else if (!session.isGameStarted()) {
                message = "GAME_NOT_STARTED";
            } else if (session.isGameFinished()) {
                message = "GAME_FINISHED";
            } else {
                message = "TURN_IN_PROGRESS";
            }
            deps.messenger().respondError(event.userId(), Type.PLACE_STONE, event.requestId(), message);
            drainPostGameSideEffects(deps, timeoutConsumer, ctx);
            return;
        }
        deps.messenger().respondMove(event.userId(), event.requestId(), result);
        if (result.status() == MoveStatus.SUCCESS) {
            deps.messenger().broadcastStonePlaced(result);
            if (result.turnSnapshot() != null && manager.currentType() == GameSessionStateType.TURN_WAITING) {
                scheduleTurnTimeout(deps, timeoutConsumer, result.session(), result.turnSnapshot());
            } else if (result.turnSnapshot() == null) {
                deps.turnTimeoutScheduler().cancel(result.session().sessionId());
            }
        }
        if (manager.currentType() == GameSessionStateType.COMPLETED) {
            cancelAllTimers(deps, result.session().sessionId());
        }
        drainPostGameSideEffects(deps, timeoutConsumer, ctx);
    }

    private static void handleTimeoutCompletion(GameSessionDependencies deps,
                                                TurnTimeoutScheduler.TurnTimeoutConsumer timeoutConsumer,
                                                GameSession session,
                                                GameStateHub manager,
                                                GameSessionStateContext ctx,
                                                TimeoutEvent event) {
        TurnTimeoutResult result = ctx.consumePendingTimeoutResult();
        if (result == null) {
            if (!session.isGameFinished()) {
                GameTurnService.TurnSnapshot snapshot =
                    deps.turnService().snapshot(session.getTurnStore());
                if (snapshot != null) {
                    scheduleTurnTimeout(deps, timeoutConsumer, session, snapshot);
                }
            }
            drainPostGameSideEffects(deps, timeoutConsumer, ctx);
            return;
        }
        boolean waiting = manager.currentType() == GameSessionStateType.TURN_WAITING;
        if (result.timedOut()) {
            deps.messenger().broadcastTurnTimeout(session, result);
            if (result.nextTurn() != null && waiting) {
                scheduleTurnTimeout(deps, timeoutConsumer, session, result.nextTurn());
            }
        } else if (result.currentTurn() != null && waiting) {
            scheduleTurnTimeout(deps, timeoutConsumer, session, result.currentTurn());
        }
        if (manager.currentType() == GameSessionStateType.COMPLETED) {
            cancelAllTimers(deps, session.sessionId());
        }
        drainPostGameSideEffects(deps, timeoutConsumer, ctx);
    }

    private static void handleDecisionTimeout(GameSessionDependencies deps,
                                              TurnTimeoutScheduler.TurnTimeoutConsumer timeoutConsumer,
                                              GameSessionId sessionId) {
        Optional<GameSession> optionalSession = deps.repository().findById(sessionId);
        if (optionalSession.isEmpty()) {
            return;
        }
        GameSession session = optionalSession.get();
        GameStateHub manager = deps.runtime().ensure(session);
        long now = System.currentTimeMillis();
        DecisionTimeoutEvent event = new DecisionTimeoutEvent(now);
        manager.submit(event, ctx -> handleDecisionTimeoutCompletion(deps, timeoutConsumer, ctx));
    }

    private static void handlePostGameDecisionCompletion(GameSessionDependencies deps,
                                                         TurnTimeoutScheduler.TurnTimeoutConsumer timeoutConsumer,
                                                         GameStateHub manager,
                                                         GameSessionStateContext ctx,
                                                         PostGameDecisionEvent event) {
        PostGameDecisionResult result = ctx.consumePendingDecisionResult();
        if (result == null) {
            result = PostGameDecisionResult.rejected(manager.session(), event.userId(),
                PostGameDecisionStatus.SESSION_CLOSED);
        }
        deps.messenger().respondPostGameDecision(event.userId(), event.requestId(), result);
        drainPostGameSideEffects(deps, timeoutConsumer,ctx);
    }

    private static void drainPostGameSideEffects(GameSessionDependencies deps,
                                                 TurnTimeoutScheduler.TurnTimeoutConsumer timeoutConsumer,
                                                 GameSessionStateContext ctx) {
        BoardSnapshotUpdate boardUpdate = ctx.consumePendingBoardSnapshot();
        if (boardUpdate != null) {
            deps.messenger().broadcastBoardSnapshot(boardUpdate);
        }
        GameCompletionNotice completion = ctx.consumePendingGameCompletion();
        if (completion != null) {
            deps.messenger().broadcastGameCompleted(completion.session());
        }
        PostGameDecisionPrompt prompt = ctx.consumePendingDecisionPrompt();
        if (prompt != null) {
            deps.messenger().broadcastPostGamePrompt(prompt);
            scheduleDecisionTimeout(deps, timeoutConsumer, prompt.session(), prompt.deadlineAt());
        }
        PostGameDecisionUpdate update = ctx.consumePendingDecisionUpdate();
        if (update != null) {
            deps.messenger().broadcastPostGameDecisionUpdate(update);
        }
        PostGameResolution resolution = ctx.consumePendingPostGameResolution();
        if (resolution != null) {
            handlePostGameResolution(deps, timeoutConsumer, resolution);
        }
    }

    private static void handlePostGameResolution(GameSessionDependencies deps,
                                                 TurnTimeoutScheduler.TurnTimeoutConsumer timeoutConsumer,
                                                 PostGameResolution resolution) {
        GameSession session = resolution.session();
        cancelAllTimers(deps, session.sessionId());
        if (resolution.type() == PostGameResolution.ResolutionType.REMATCH) {
            handleRematchResolution(deps, resolution);
        } else {
            handleSessionTermination(deps, session, resolution.disconnected());
        }
    }

    private static void handleRematchResolution(GameSessionDependencies deps,
                                                PostGameResolution resolution) {
        GameSession oldSession = resolution.session();
        List<String> participants = resolution.rematchParticipants();
        if (participants.size() < 2) {
            handleSessionTermination(deps, oldSession, resolution.disconnected());
            return;
        }
        GameSession newSession = new GameSession(participants);
        newSession.setRulesContext(deps.ruleManager().prepareRules(newSession));
        deps.repository().save(newSession);
        deps.runtime().ensure(newSession);
        deps.messenger().broadcastRematchStarted(oldSession, newSession, participants);
        deps.messenger().broadcastJoin(newSession);
        deps.messenger().broadcastSessionTerminated(oldSession, resolution.disconnected());
        deps.repository().removeById(oldSession.sessionId()).ifPresent(deps.runtime()::remove);
    }

    private static void handleSessionTermination(GameSessionDependencies deps,
                                                 GameSession session,
                                                 List<String> disconnected) {
        deps.messenger().broadcastSessionTerminated(session, disconnected);
        deps.repository().removeById(session.sessionId()).ifPresent(deps.runtime()::remove);
    }

    private static void scheduleTurnTimeout(GameSessionDependencies deps,
                                            TurnTimeoutScheduler.TurnTimeoutConsumer timeoutConsumer,
                                            GameSession session,
                                            GameTurnService.TurnSnapshot turnSnapshot) {
        deps.turnTimeoutScheduler().schedule(session, turnSnapshot, timeoutConsumer);
    }

    private static void scheduleDecisionTimeout(GameSessionDependencies deps,
                                                TurnTimeoutScheduler.TurnTimeoutConsumer timeoutConsumer,
                                                GameSession session,
                                                long deadlineAt) {
        GameSessionId sessionId = session.sessionId();
        deps.decisionTimeoutScheduler().schedule(sessionId, deadlineAt,
            () -> handleDecisionTimeout(deps, timeoutConsumer, sessionId));
    }

    private static void handleDecisionTimeoutCompletion(GameSessionDependencies deps,
                                                        TurnTimeoutScheduler.TurnTimeoutConsumer timeoutConsumer,
                                                        GameSessionStateContext ctx) {
        drainPostGameSideEffects(deps, timeoutConsumer, ctx);
    }
}
