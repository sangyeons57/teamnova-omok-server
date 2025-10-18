package teamnova.omok.glue.game.session.services;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import teamnova.omok.glue.game.session.model.PostGameDecision;
import teamnova.omok.glue.game.session.interfaces.DecisionTimeoutScheduler;
import teamnova.omok.glue.game.session.interfaces.GameSessionEventProcessor;
import teamnova.omok.glue.game.session.interfaces.GameSessionMessenger;
import teamnova.omok.glue.game.session.interfaces.GameSessionRepository;
import teamnova.omok.glue.game.session.interfaces.GameSessionRuntime;
import teamnova.omok.glue.game.session.interfaces.GameTurnService;
import teamnova.omok.glue.game.session.interfaces.TurnTimeoutScheduler;
import teamnova.omok.glue.game.session.model.GameSession;
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
import teamnova.omok.glue.service.dto.BoardSnapshotUpdate;
import teamnova.omok.glue.service.dto.GameCompletionNotice;
import teamnova.omok.glue.service.dto.MoveResult;
import teamnova.omok.glue.service.dto.MoveStatus;
import teamnova.omok.glue.service.dto.PostGameDecisionPrompt;
import teamnova.omok.glue.service.dto.PostGameDecisionResult;
import teamnova.omok.glue.service.dto.PostGameDecisionStatus;
import teamnova.omok.glue.service.dto.PostGameDecisionUpdate;
import teamnova.omok.glue.service.dto.PostGameResolution;
import teamnova.omok.glue.service.dto.ReadyResult;
import teamnova.omok.glue.service.dto.TurnTimeoutResult;

/**
 * Handles state-machine event submission, completion processing, and associated side effects.
 */
public final class SessionEventService implements GameSessionEventProcessor, TurnTimeoutScheduler.TurnTimeoutConsumer {
    private final GameSessionRepository repository;
    private final GameSessionRuntime runtime;
    private final GameTurnService turnService;
    private final GameSessionMessenger publisher;
    private final TurnTimeoutScheduler timeoutCoordinator;
    private final DecisionTimeoutScheduler decisionTimeoutCoordinator;
    private final teamnova.omok.glue.rule.RuleManager ruleManager;

    public SessionEventService(GameSessionRepository repository,
                        GameSessionRuntime runtime,
                        GameTurnService turnService,
                        GameSessionMessenger publisher,
                        TurnTimeoutScheduler timeoutCoordinator,
                        DecisionTimeoutScheduler decisionTimeoutCoordinator,
                        teamnova.omok.glue.rule.RuleManager ruleManager) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.runtime = Objects.requireNonNull(runtime, "runtime");
        this.turnService = Objects.requireNonNull(turnService, "turnService");
        this.publisher = Objects.requireNonNull(publisher, "publisher");
        this.timeoutCoordinator = Objects.requireNonNull(timeoutCoordinator, "timeoutCoordinator");
        this.decisionTimeoutCoordinator = Objects.requireNonNull(decisionTimeoutCoordinator, "decisionTimeoutCoordinator");
        this.ruleManager = Objects.requireNonNull(ruleManager, "ruleManager");
    }

    @Override
    public boolean submitReady(String userId, long requestId) {
        return withSession(userId, ctx -> {
            ReadyEvent event = new ReadyEvent(userId, ctx.timestamp(), requestId);
            ctx.manager().submit(event, stateCtx -> handleReadyCompletion(ctx.manager(), stateCtx, event));
        });
    }

    @Override
    public boolean submitMove(String userId, long requestId, int x, int y) {
        return withSession(userId, ctx -> {
            MoveEvent event = new MoveEvent(userId, x, y, ctx.timestamp(), requestId);
            ctx.manager().submit(event, stateCtx -> handleMoveCompletion(ctx.manager(), stateCtx, event));
        });
    }

    @Override
    public boolean submitPostGameDecision(String userId, long requestId, PostGameDecision decision) {
        Objects.requireNonNull(decision, "decision");
        return withSession(userId, ctx -> {
            PostGameDecisionEvent event = new PostGameDecisionEvent(userId, decision, ctx.timestamp(), requestId);
            ctx.manager().submit(event, stateCtx -> handlePostGameDecisionCompletion(ctx.manager(), stateCtx, event));
        });
    }

    @Override
    public void cancelAllTimers(GameSessionId sessionId) {
        timeoutCoordinator.cancel(sessionId);
        decisionTimeoutCoordinator.cancel(sessionId);
        runtime.remove(sessionId);
    }

    GameTurnService.TurnSnapshot turnSnapshot(GameSession session) {
        return turnService.snapshot(session.getTurnStore(), session.getUserIds());
    }

    boolean isTurnExpired(GameSession session, long now) {
        return turnService.isExpired(session.getTurnStore(), now);
    }

    private boolean withSession(String userId, Consumer<SessionSubmission> consumer) {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(consumer, "consumer");
        Optional<GameSession> optionalSession = repository.findByUserId(userId);
        if (optionalSession.isEmpty()) {
            return false;
        }
        GameSession session = optionalSession.get();
        GameStateHub manager = runtime.ensure(session);
        long now = System.currentTimeMillis();
        consumer.accept(new SessionSubmission(session, manager, now));
        return true;
    }

    private void handleReadyCompletion(GameStateHub manager,
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
            cancelAllTimers(result.session().sessionId());
        }
        drainPostGameSideEffects(manager, ctx);
    }

    private void handleMoveCompletion(GameStateHub manager,
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
            timeoutCoordinator.cancel(result.session().sessionId());
            }
        }
        if (manager.currentType() == GameSessionStateType.COMPLETED) {
            cancelAllTimers(result.session().sessionId());
        }
        drainPostGameSideEffects(manager, ctx);
    }

    private void handleTimeoutCompletion(GameSession session,
                                         GameStateHub manager,
                                         GameSessionStateContext ctx,
                                         TimeoutEvent event) {
        TurnTimeoutResult result = ctx.consumePendingTimeoutResult();
        if (result == null) {
            if (!session.isGameFinished()) {
                GameTurnService.TurnSnapshot snapshot = turnService.snapshot(session.getTurnStore(), session.getUserIds());
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
            cancelAllTimers(session.sessionId());
        }
        drainPostGameSideEffects(manager, ctx);
    }

    @Override
    public void onTimeout(GameSessionId sessionId, int expectedTurnNumber) {
        handleScheduledTimeout(sessionId, expectedTurnNumber);
    }

    private void handleScheduledTimeout(GameSessionId sessionId, int expectedTurnNumber) {
        if (!timeoutCoordinator.validate(sessionId, expectedTurnNumber)) {
            return;
        }
        Optional<GameSession> optionalSession = repository.findById(sessionId);
        if (optionalSession.isEmpty()) {
            return;
        }
        GameSession session = optionalSession.get();
        GameStateHub manager = runtime.ensure(session);
        timeoutCoordinator.clearIfMatches(sessionId, expectedTurnNumber);
        long now = System.currentTimeMillis();
        TimeoutEvent event = new TimeoutEvent(expectedTurnNumber, now);
        manager.submit(event, ctx -> handleTimeoutCompletion(session, manager, ctx, event));
    }

    @Override
    public void skipTurnForDisconnected(GameSession session, String userId, int expectedTurnNumber) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(userId, "userId");
        boolean shouldSubmit = false;
        session.lock().lock();
        try {
            if (session.isGameStarted() && !session.isGameFinished()) {
                GameTurnService.TurnSnapshot current =
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
        timeoutCoordinator.cancel(session.sessionId());
        GameStateHub manager = runtime.ensure(session);
        long now = System.currentTimeMillis();
        TimeoutEvent event = new TimeoutEvent(expectedTurnNumber, now);
        manager.submit(event, ctx -> handleTimeoutCompletion(session, manager, ctx, event));
    }

    private void handleDecisionTimeout(GameSessionId sessionId) {
        Optional<GameSession> optionalSession = repository.findById(sessionId);
        if (optionalSession.isEmpty()) {
            return;
        }
        GameSession session = optionalSession.get();
        GameStateHub manager = runtime.ensure(session);
        long now = System.currentTimeMillis();
        DecisionTimeoutEvent event = new DecisionTimeoutEvent(now);
        manager.submit(event, ctx -> handleDecisionTimeoutCompletion(manager, ctx));
    }

    private void handleDecisionTimeoutCompletion(GameStateHub manager,
                                                 GameSessionStateContext ctx) {
        drainPostGameSideEffects(manager, ctx);
    }

    private void handlePostGameDecisionCompletion(GameStateHub manager,
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

    private void drainPostGameSideEffects(GameStateHub manager,
                                          GameSessionStateContext ctx) {
        BoardSnapshotUpdate boardUpdate = ctx.consumePendingBoardSnapshot();
        if (boardUpdate != null) {
            publisher.broadcastBoardSnapshot(boardUpdate);
        }
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
        cancelAllTimers(session.sessionId());
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
        newSession.setRulesContext(ruleManager.prepareRules(newSession));
        repository.save(newSession);
        runtime.ensure(newSession);
        publisher.broadcastRematchStarted(oldSession, newSession, participants);
        publisher.broadcastJoin(newSession);
        publisher.broadcastSessionTerminated(oldSession, resolution.disconnected());
        repository.removeById(oldSession.sessionId()).ifPresent(runtime::remove);
    }

    private void handleSessionTermination(GameSession session, List<String> disconnected) {
        publisher.broadcastSessionTerminated(session, disconnected);
        repository.removeById(session.sessionId()).ifPresent(runtime::remove);
    }

    private void scheduleTurnTimeout(GameSession session, GameTurnService.TurnSnapshot turnSnapshot) {
        timeoutCoordinator.schedule(session, turnSnapshot, this);
    }

    private void scheduleDecisionTimeout(GameSession session, long deadlineAt) {
        GameSessionId sessionId = session.sessionId();
        decisionTimeoutCoordinator.schedule(sessionId, deadlineAt, () -> handleDecisionTimeout(sessionId));
    }

}
