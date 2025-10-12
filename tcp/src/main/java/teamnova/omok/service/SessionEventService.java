package teamnova.omok.service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import teamnova.omok.game.PostGameDecision;
import teamnova.omok.handler.register.Type;
import teamnova.omok.service.cordinator.DecisionTimeoutCoordinator;
import teamnova.omok.service.cordinator.TurnTimeoutCoordinator;
import teamnova.omok.service.dto.GameCompletionNotice;
import teamnova.omok.service.dto.MoveResult;
import teamnova.omok.service.dto.MoveStatus;
import teamnova.omok.service.dto.PostGameDecisionPrompt;
import teamnova.omok.service.dto.PostGameDecisionResult;
import teamnova.omok.service.dto.PostGameDecisionStatus;
import teamnova.omok.service.dto.PostGameDecisionUpdate;
import teamnova.omok.service.dto.PostGameResolution;
import teamnova.omok.service.dto.ReadyResult;
import teamnova.omok.service.dto.TurnTimeoutResult;
import teamnova.omok.state.game.event.DecisionTimeoutEvent;
import teamnova.omok.state.game.event.MoveEvent;
import teamnova.omok.state.game.event.PostGameDecisionEvent;
import teamnova.omok.state.game.event.ReadyEvent;
import teamnova.omok.state.game.event.TimeoutEvent;
import teamnova.omok.state.game.manage.GameSessionStateContext;
import teamnova.omok.state.game.manage.GameSessionStateManager;
import teamnova.omok.state.game.manage.GameSessionStateType;
import teamnova.omok.store.GameSession;
import teamnova.omok.store.InGameSessionStore;

/**
 * Handles state-machine event submission, completion processing, and associated side effects.
 */
final class SessionEventService implements TurnTimeoutCoordinator.TurnTimeoutConsumer {
    private final InGameSessionStore store;
    private final TurnService turnService;
    private final SessionMessagePublisher publisher;
    private final TurnTimeoutCoordinator timeoutCoordinator;
    private final DecisionTimeoutCoordinator decisionTimeoutCoordinator;
    private final ScoreService scoreService;
    private final RuleService ruleService;

    SessionEventService(InGameSessionStore store,
                        TurnService turnService,
                        SessionMessagePublisher publisher,
                        TurnTimeoutCoordinator timeoutCoordinator,
                        DecisionTimeoutCoordinator decisionTimeoutCoordinator,
                        ScoreService scoreService,
                        RuleService ruleService) {
        this.store = Objects.requireNonNull(store, "store");
        this.turnService = Objects.requireNonNull(turnService, "turnService");
        this.publisher = Objects.requireNonNull(publisher, "publisher");
        this.timeoutCoordinator = Objects.requireNonNull(timeoutCoordinator, "timeoutCoordinator");
        this.decisionTimeoutCoordinator = Objects.requireNonNull(decisionTimeoutCoordinator, "decisionTimeoutCoordinator");
        this.scoreService = Objects.requireNonNull(scoreService, "scoreService");
        this.ruleService = Objects.requireNonNull(ruleService, "ruleService");
    }

    boolean submitReady(String userId, long requestId) {
        return withSession(userId, ctx -> {
            ReadyEvent event = new ReadyEvent(userId, ctx.now(), requestId);
            ctx.manager().submit(event, stateCtx -> handleReadyCompletion(ctx.manager(), stateCtx, event));
        });
    }

    boolean submitMove(String userId, long requestId, int x, int y) {
        return withSession(userId, ctx -> {
            MoveEvent event = new MoveEvent(userId, x, y, ctx.now(), requestId);
            ctx.manager().submit(event, stateCtx -> handleMoveCompletion(ctx.manager(), stateCtx, event));
        });
    }

    boolean submitPostGameDecision(String userId, long requestId, PostGameDecision decision) {
        Objects.requireNonNull(decision, "decision");
        return withSession(userId, ctx -> {
            PostGameDecisionEvent event = new PostGameDecisionEvent(userId, decision, ctx.now(), requestId);
            ctx.manager().submit(event, stateCtx -> handlePostGameDecisionCompletion(ctx.manager(), stateCtx, event));
        });
    }

    void cancelAllTimers(UUID sessionId) {
        timeoutCoordinator.cancel(sessionId);
        decisionTimeoutCoordinator.cancel(sessionId);
    }

    TurnService.TurnSnapshot turnSnapshot(GameSession session) {
        return turnService.snapshot(session.getTurnStore(), session.getUserIds());
    }

    boolean isTurnExpired(GameSession session, long now) {
        return turnService.isExpired(session.getTurnStore(), now);
    }

    private boolean withSession(String userId, Consumer<SessionSubmissionContext> consumer) {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(consumer, "consumer");
        Optional<GameSession> optionalSession = store.findByUserId(userId);
        if (optionalSession.isEmpty()) {
            return false;
        }
        GameSession session = optionalSession.get();
        if (session.getRulesContext() == null) {
            session.setRulesContext(ruleService.prepareRules(session));
        }
        GameSessionStateManager manager = store.ensureManager(session);
        long now = System.currentTimeMillis();
        consumer.accept(new SessionSubmissionContext(session, manager, now));
        return true;
    }

    private void handleReadyCompletion(GameSessionStateManager manager,
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
            publisher.respondError(event.userId(), Type.READY_IN_GAME_SESSION, event.requestId(), message);
            drainPostGameSideEffects(manager, ctx);
            return;
        }
        if (!result.validUser()) {
            publisher.respondError(event.userId(), Type.READY_IN_GAME_SESSION, event.requestId(), "INVALID_PLAYER");
            drainPostGameSideEffects(manager, ctx);
            return;
        }
        publisher.respondReady(event.userId(), event.requestId(), result);
        if (result.stateChanged()) {
            publisher.broadcastReady(result);
        }
        if (result.gameStartedNow() && result.firstTurn() != null) {
            publisher.broadcastGameStart(result.session(), result.firstTurn());
            scheduleTurnTimeout(result.session(), result.firstTurn());
        }
        if (manager.currentType() == GameSessionStateType.COMPLETED) {
            cancelAllTimers(result.session().getId());
        }
        drainPostGameSideEffects(manager, ctx);
    }

    private void handleMoveCompletion(GameSessionStateManager manager,
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
            publisher.respondError(event.userId(), Type.PLACE_STONE, event.requestId(), message);
            drainPostGameSideEffects(manager, ctx);
            return;
        }
        publisher.respondMove(event.userId(), event.requestId(), result);
        if (result.status() == MoveStatus.SUCCESS) {
            publisher.broadcastStonePlaced(result);
            if (result.turnSnapshot() != null && manager.currentType() == GameSessionStateType.TURN_WAITING) {
                scheduleTurnTimeout(result.session(), result.turnSnapshot());
            } else if (result.turnSnapshot() == null) {
                timeoutCoordinator.cancel(result.session().getId());
            }
        }
        if (manager.currentType() == GameSessionStateType.COMPLETED) {
            cancelAllTimers(result.session().getId());
        }
        drainPostGameSideEffects(manager, ctx);
    }

    private void handleTimeoutCompletion(GameSession session,
                                         GameSessionStateManager manager,
                                         GameSessionStateContext ctx,
                                         TimeoutEvent event) {
        TurnTimeoutResult result = ctx.consumePendingTimeoutResult();
        if (result == null) {
            if (!session.isGameFinished()) {
                TurnService.TurnSnapshot snapshot = turnService.snapshot(session.getTurnStore(), session.getUserIds());
                if (snapshot != null) {
                    scheduleTurnTimeout(session, snapshot);
                }
            }
            drainPostGameSideEffects(manager, ctx);
            return;
        }
        boolean waiting = manager.currentType() == GameSessionStateType.TURN_WAITING;
        if (result.timedOut()) {
            publisher.broadcastTurnTimeout(session, result);
            if (result.nextTurn() != null && waiting) {
                scheduleTurnTimeout(session, result.nextTurn());
            }
        } else if (result.currentTurn() != null && waiting) {
            scheduleTurnTimeout(session, result.currentTurn());
        }
        if (manager.currentType() == GameSessionStateType.COMPLETED) {
            cancelAllTimers(session.getId());
        }
        drainPostGameSideEffects(manager, ctx);
    }

    @Override
    public void onTimeout(UUID sessionId, int expectedTurnNumber) {
        handleScheduledTimeout(sessionId, expectedTurnNumber);
    }

    private void handleScheduledTimeout(UUID sessionId, int expectedTurnNumber) {
        if (!timeoutCoordinator.validate(sessionId, expectedTurnNumber)) {
            return;
        }
        Optional<GameSession> optionalSession = store.findById(sessionId);
        if (optionalSession.isEmpty()) {
            return;
        }
        GameSession session = optionalSession.get();
        GameSessionStateManager manager = store.ensureManager(session);
        timeoutCoordinator.clearIfMatches(sessionId, expectedTurnNumber);
        long now = System.currentTimeMillis();
        TimeoutEvent event = new TimeoutEvent(expectedTurnNumber, now);
        manager.submit(event, ctx -> handleTimeoutCompletion(session, manager, ctx, event));
    }

    void skipTurnForDisconnected(GameSession session, String userId, int expectedTurnNumber) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(userId, "userId");
        boolean shouldSubmit = false;
        session.lock().lock();
        try {
            if (session.isGameStarted() && !session.isGameFinished()) {
                TurnService.TurnSnapshot current =
                    turnService.snapshot(session.getTurnStore(), session.getUserIds());
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
        timeoutCoordinator.cancel(session.getId());
        GameSessionStateManager manager = store.ensureManager(session);
        long now = System.currentTimeMillis();
        TimeoutEvent event = new TimeoutEvent(expectedTurnNumber, now);
        manager.submit(event, ctx -> handleTimeoutCompletion(session, manager, ctx, event));
    }

    private void handleDecisionTimeout(UUID sessionId) {
        Optional<GameSession> optionalSession = store.findById(sessionId);
        if (optionalSession.isEmpty()) {
            return;
        }
        GameSession session = optionalSession.get();
        GameSessionStateManager manager = store.ensureManager(session);
        long now = System.currentTimeMillis();
        DecisionTimeoutEvent event = new DecisionTimeoutEvent(now);
        manager.submit(event, ctx -> handleDecisionTimeoutCompletion(manager, ctx));
    }

    private void handleDecisionTimeoutCompletion(GameSessionStateManager manager,
                                                 GameSessionStateContext ctx) {
        drainPostGameSideEffects(manager, ctx);
    }

    private void handlePostGameDecisionCompletion(GameSessionStateManager manager,
                                                  GameSessionStateContext ctx,
                                                  PostGameDecisionEvent event) {
        PostGameDecisionResult result = ctx.consumePendingDecisionResult();
        if (result == null) {
            result = PostGameDecisionResult.rejected(manager.session(), event.userId(),
                PostGameDecisionStatus.SESSION_CLOSED);
        }
        publisher.respondPostGameDecision(event.userId(), event.requestId(), result);
        drainPostGameSideEffects(manager, ctx);
    }

    private void drainPostGameSideEffects(GameSessionStateManager manager,
                                          GameSessionStateContext ctx) {
        GameCompletionNotice completion = ctx.consumePendingGameCompletion();
        if (completion != null) {
            publisher.broadcastGameCompleted(completion.session());
        }
        PostGameDecisionPrompt prompt = ctx.consumePendingDecisionPrompt();
        if (prompt != null) {
            publisher.broadcastPostGamePrompt(prompt);
            scheduleDecisionTimeout(prompt.session(), prompt.deadlineAt());
        }
        PostGameDecisionUpdate update = ctx.consumePendingDecisionUpdate();
        if (update != null) {
            publisher.broadcastPostGameDecisionUpdate(update);
        }
        PostGameResolution resolution = ctx.consumePendingPostGameResolution();
        if (resolution != null) {
            handlePostGameResolution(resolution);
        }
    }

    private void handlePostGameResolution(PostGameResolution resolution) {
        GameSession session = resolution.session();
        scoreService.applyGameResults(session);
        cancelAllTimers(session.getId());
        if (resolution.type() == PostGameResolution.ResolutionType.REMATCH) {
            handleRematchResolution(resolution);
        } else {
            handleSessionTermination(session, resolution.disconnected());
        }
    }

    private void handleRematchResolution(PostGameResolution resolution) {
        GameSession oldSession = resolution.session();
        List<String> participants = resolution.rematchParticipants();
        if (participants.size() < 2) {
            handleSessionTermination(oldSession, resolution.disconnected());
            return;
        }
        GameSession newSession = new GameSession(participants);
        newSession.setRulesContext(ruleService.prepareRules(newSession));
        store.save(newSession);
        publisher.broadcastRematchStarted(oldSession, newSession, participants);
        publisher.broadcastJoin(newSession);
        publisher.broadcastSessionTerminated(oldSession, resolution.disconnected());
        store.removeById(oldSession.getId());
    }

    private void handleSessionTermination(GameSession session, List<String> disconnected) {
        publisher.broadcastSessionTerminated(session, disconnected);
        store.removeById(session.getId());
    }

    private void scheduleTurnTimeout(GameSession session, TurnService.TurnSnapshot turnSnapshot) {
        timeoutCoordinator.schedule(session, turnSnapshot, this);
    }

    private void scheduleDecisionTimeout(GameSession session, long deadlineAt) {
        decisionTimeoutCoordinator.schedule(session.getId(), deadlineAt, () -> handleDecisionTimeout(session.getId()));
    }

    private record SessionSubmissionContext(GameSession session,
                                            GameSessionStateManager manager,
                                            long now) { }
}
