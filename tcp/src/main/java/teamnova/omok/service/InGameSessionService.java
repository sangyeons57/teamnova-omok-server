package teamnova.omok.service;

import teamnova.omok.handler.register.Type;
import teamnova.omok.game.PlayerResult;
import teamnova.omok.game.PostGameDecision;
import teamnova.omok.message.encoder.ErrorMessageEncoder;
import teamnova.omok.message.encoder.GameSessionCompletedMessageEncoder;
import teamnova.omok.message.encoder.GameSessionRematchStartedMessageEncoder;
import teamnova.omok.message.encoder.GameSessionStartedMessageEncoder;
import teamnova.omok.message.encoder.GameSessionTerminatedMessageEncoder;
import teamnova.omok.message.encoder.JoinSessionMessageEncoder;
import teamnova.omok.message.encoder.MoveAckMessageEncoder;
import teamnova.omok.message.encoder.PostGameDecisionAckMessageEncoder;
import teamnova.omok.message.encoder.PostGameDecisionPromptMessageEncoder;
import teamnova.omok.message.encoder.PostGameDecisionUpdateMessageEncoder;
import teamnova.omok.message.encoder.ReadyStateMessageEncoder;
import teamnova.omok.message.encoder.StonePlacedMessageEncoder;
import teamnova.omok.message.encoder.TurnTimeoutMessageEncoder;
import teamnova.omok.nio.ClientSession;
import teamnova.omok.nio.NioReactorServer;
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
import teamnova.omok.store.Stone;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InGameSessionService {
    private final InGameSessionStore store;
    private final BoardService boardService;
    private final TurnService turnService;
    private final ConcurrentMap<String, ClientSession> clients = new ConcurrentHashMap<>();
    private final TurnTimeoutCoordinator timeoutCoordinator;
    private final DecisionTimeoutCoordinator decisionTimeoutCoordinator;
    /*
     * Post-game networking contract (planned):
     *
     * Broadcast: GAME_SESSION_COMPLETED (already live)
     *   {
     *     "sessionId": "<uuid>",
     *     "outcomes": [
     *       { "userId": "<id>", "result": "WIN|LOSS|DRAW" }
     *     ]
     *   }
     *
     * Broadcast: GAME_POST_DECISION_PROMPT
     *   {
     *     "sessionId": "<uuid>",
     *     "deadlineAt": <epochMillis>,
     *     "options": ["REMATCH", "LEAVE"],
     *     "autoAction": "LEAVE"  // what happens if no reply before deadline
     *   }
     *
     * Client request: POST_GAME_DECISION
     *   {
     *     "sessionId": "<uuid>",
     *     "decision": "REMATCH|LEAVE"
     *   }
     *
     * Server response (ack/error) on POST_GAME_DECISION
     *   Success:
     *     { "status": "OK" }
     *   Error:
     *     { "status": "ERROR", "reason": "INVALID_PLAYER|TIMEOUT|ALREADY_DECIDED|SESSION_CLOSED|SESSION_NOT_FOUND|INVALID_PAYLOAD" }
     *
     * Optional broadcast: GAME_POST_DECISION_UPDATE
     *   {
     *     "sessionId": "<uuid>",
     *     "decided": [
     *       { "userId": "<id>", "decision": "REMATCH|LEAVE" }
     *     ],
     *     "remaining": ["<userId>", ...]
     *   }
     *
     * Broadcast: GAME_SESSION_REMATCH_STARTED
     *   {
     *     "sessionId": "<previousUuid>",
     *     "rematchSessionId": "<newUuid>",
     *     "participants": ["<userId>", ...]
     *   }
     *
     * Broadcast: GAME_SESSION_TERMINATED
     *   {
     *     "sessionId": "<uuid>",
     *     "disconnected": ["<userId>", ...]
     *   }
     *
     * Planned state flow:
     *   1. OutcomeEvaluatingState ⇒ POST_GAME_DECISION_WAITING (new). On enter: GAME_SESSION_COMPLETED + GAME_POST_DECISION_PROMPT; start 30s timer.
     *   2. Clients send POST_GAME_DECISION; state records REMATCH/LEAVE and can emit GAME_POST_DECISION_UPDATE.
     *   3. When all decide or timer hits (DecisionTimeoutEvent), transition ⇒ POST_GAME_DECISION_RESOLVING.
     *   4. POST_GAME_DECISION_RESOLVING:
     *        • rematchCount ≥ 2 ⇒ SESSION_REMATCH_PREPARING → spawn new session, send GAME_SESSION_REMATCH_STARTED.
     *        • otherwise ⇒ SESSION_TERMINATING → send GAME_SESSION_TERMINATED and drop session from store.
     */

    private volatile NioReactorServer server;

    public InGameSessionService(InGameSessionStore store,
                                BoardService boardService,
                                TurnService turnService,
                                OutcomeService outcomeService) {
        Objects.requireNonNull(outcomeService, "outcomeService");
        this.store = Objects.requireNonNull(store, "store");
        this.boardService = Objects.requireNonNull(boardService, "boardService");
        this.turnService = Objects.requireNonNull(turnService, "turnService");
        this.timeoutCoordinator = new TurnTimeoutCoordinator();
        this.decisionTimeoutCoordinator = new DecisionTimeoutCoordinator();
    }

    public void attachServer(NioReactorServer server) {
        this.server = Objects.requireNonNull(server, "server");
    }

    public void registerClient(String userId, ClientSession session) {
        if (userId != null && session != null) {
            clients.put(userId, session);
        }
    }

    public void unregisterClient(String userId) {
        if (userId != null) clients.remove(userId);
    }

    public Optional<GameSession> findByUser(String userId) {
        return store.findByUserId(userId);
    }

    public ClientSession getClient(String userId) {
        return clients.get(userId);
    }

    public void leaveByUser(String userId) {
        store.findByUserId(userId).ifPresent(session -> {
            timeoutCoordinator.cancel(session.getId());
            decisionTimeoutCoordinator.cancel(session.getId());
            session.lock().lock();
            try {
                session.markDisconnected(userId);
                session.updateOutcome(userId, PlayerResult.LOSS);
            } finally {
                session.lock().unlock();
            }
        });
        store.removeByUserId(userId);
    }

    public boolean submitReady(String userId, long requestId) {
        Objects.requireNonNull(userId, "userId");
        Optional<GameSession> optionalSession = store.findByUserId(userId);
        if (optionalSession.isEmpty()) {
            return false;
        }
        GameSession session = optionalSession.get();
        GameSessionStateManager manager = store.ensureManager(session);
        long now = System.currentTimeMillis();
        ReadyEvent event = new ReadyEvent(userId, now, requestId);
        manager.submit(event, ctx -> handleReadyCompletion(manager, ctx, event));
        return true;
    }

    public boolean submitMove(String userId, long requestId, int x, int y) {
        Objects.requireNonNull(userId, "userId");
        Optional<GameSession> optionalSession = store.findByUserId(userId);
        if (optionalSession.isEmpty()) {
            return false;
        }
        GameSession session = optionalSession.get();
        GameSessionStateManager manager = store.ensureManager(session);
        long now = System.currentTimeMillis();
        MoveEvent event = new MoveEvent(userId, x, y, now, requestId);
        manager.submit(event, ctx -> handleMoveCompletion(manager, ctx, event));
        return true;
    }

    public boolean submitPostGameDecision(String userId, long requestId, PostGameDecision decision) {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(decision, "decision");
        Optional<GameSession> optionalSession = store.findByUserId(userId);
        if (optionalSession.isEmpty()) {
            return false;
        }
        GameSession session = optionalSession.get();
        GameSessionStateManager manager = store.ensureManager(session);
        long now = System.currentTimeMillis();
        PostGameDecisionEvent event = new PostGameDecisionEvent(userId, decision, now, requestId);
        manager.submit(event, ctx -> handlePostGameDecisionCompletion(manager, ctx, event));
        return true;
    }

    public GameSession createFromGroup(NioReactorServer server, MatchingService.Group group) {
        attachServer(server);
        List<String> userIds = new ArrayList<>();
        group.getTickets().forEach(t -> userIds.add(t.id));
        GameSession session = new GameSession(userIds);
        store.save(session);
        broadcastJoin(server, session);
        return session;
    }

    private void broadcastJoin(NioReactorServer server, GameSession session) {
        byte[] payload = JoinSessionMessageEncoder.encode(session);
        broadcastToSession(server, session, Type.JOIN_IN_GAME_SESSION, payload);
    }

    private void broadcastReady(ReadyResult result) {
        NioReactorServer srv = server;
        if (srv == null) return;
        byte[] payload = ReadyStateMessageEncoder.encode(result);
        broadcastToSession(srv, result.session(), Type.READY_IN_GAME_SESSION, payload);
    }

    private void broadcastGameStart(GameSession session, TurnService.TurnSnapshot turn) {
        NioReactorServer srv = server;
        if (srv == null) return;
        byte[] payload = GameSessionStartedMessageEncoder.encode(session, turn);
        broadcastToSession(srv, session, Type.GAME_SESSION_STARTED, payload);
    }

    private void broadcastStonePlaced(MoveResult result) {
        NioReactorServer srv = server;
        if (srv == null) return;
        byte[] payload = StonePlacedMessageEncoder.encode(result);
        broadcastToSession(srv, result.session(), Type.STONE_PLACED, payload);
    }

    private void broadcastTurnTimeout(GameSession session, TurnTimeoutResult result) {
        NioReactorServer srv = server;
        if (srv == null) return;
        byte[] payload = TurnTimeoutMessageEncoder.encode(result);
        broadcastToSession(srv, session, Type.TURN_TIMEOUT, payload);
    }

    private void broadcastGameCompleted(GameSession session) {
        NioReactorServer srv = server;
        if (srv == null) return;
        byte[] payload = GameSessionCompletedMessageEncoder.encode(session);
        broadcastToSession(srv, session, Type.GAME_SESSION_COMPLETED, payload);
    }

    private void broadcastPostGamePrompt(PostGameDecisionPrompt prompt) {
        NioReactorServer srv = server;
        if (srv == null) return;
        byte[] payload = PostGameDecisionPromptMessageEncoder.encode(prompt);
        broadcastToSession(srv, prompt.session(), Type.GAME_POST_DECISION_PROMPT, payload);
    }

    private void broadcastPostGameDecisionUpdate(PostGameDecisionUpdate update) {
        NioReactorServer srv = server;
        if (srv == null) return;
        byte[] payload = PostGameDecisionUpdateMessageEncoder.encode(update);
        broadcastToSession(srv, update.session(), Type.GAME_POST_DECISION_UPDATE, payload);
    }

    private void broadcastSessionTerminated(GameSession session, List<String> disconnected) {
        NioReactorServer srv = server;
        if (srv == null) return;
        byte[] payload = GameSessionTerminatedMessageEncoder.encode(session, disconnected);
        broadcastToSession(srv, session, Type.GAME_SESSION_TERMINATED, payload);
    }

    private void broadcastRematchStarted(GameSession previous, GameSession rematch, List<String> participants) {
        NioReactorServer srv = server;
        if (srv == null) return;
        byte[] payload = GameSessionRematchStartedMessageEncoder.encode(previous, rematch, participants);
        broadcastToSession(srv, previous, Type.GAME_SESSION_REMATCH_STARTED, payload);
    }

    private void respondPostGameDecision(String userId,
                                         long requestId,
                                         PostGameDecisionResult result) {
        NioReactorServer srv = server;
        if (srv == null) {
            return;
        }
        ClientSession cs = clients.get(userId);
        if (cs == null) {
            return;
        }
        byte[] payload = PostGameDecisionAckMessageEncoder.encode(result);
        cs.enqueueResponse(Type.POST_GAME_DECISION, requestId, payload);
        srv.enqueueSelectorTask(cs::enableWriteInterest);
    }

    private void respondToUser(String userId, Type type, long requestId, byte[] payload) {
        NioReactorServer srv = server;
        if (srv == null) {
            return;
        }
        ClientSession cs = clients.get(userId);
        if (cs == null) {
            return;
        }
        cs.enqueueResponse(type, requestId, payload);
        srv.enqueueSelectorTask(cs::enableWriteInterest);
    }

    private void respondError(String userId, Type type, long requestId, String message) {
        respondToUser(userId, type, requestId, ErrorMessageEncoder.encode(message));
    }

    private void broadcastToSession(NioReactorServer srv, GameSession session, Type type, byte[] payload) {
        for (String uid : session.getUserIds()) {
            ClientSession cs = clients.get(uid);
            if (cs != null) {
                cs.enqueueResponse(type, 0L, payload);
                srv.enqueueSelectorTask(cs::enableWriteInterest);
            }
        }
    }

    private void scheduleTurnTimeout(GameSession session, TurnService.TurnSnapshot turnSnapshot) {
        timeoutCoordinator.schedule(session, turnSnapshot, this::handleScheduledTimeout);
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
            respondError(event.userId(), Type.READY_IN_GAME_SESSION, event.requestId(), message);
            drainPostGameSideEffects(manager, ctx);
            return;
        }
        if (!result.validUser()) {
            respondError(event.userId(), Type.READY_IN_GAME_SESSION, event.requestId(), "INVALID_PLAYER");
            drainPostGameSideEffects(manager, ctx);
            return;
        }
        respondToUser(
            event.userId(),
            Type.READY_IN_GAME_SESSION,
            event.requestId(),
            ReadyStateMessageEncoder.encode(result)
        );
        if (result.stateChanged()) {
            broadcastReady(result);
        }
        if (result.gameStartedNow() && result.firstTurn() != null) {
            broadcastGameStart(result.session(), result.firstTurn());
            scheduleTurnTimeout(result.session(), result.firstTurn());
        }
        if (manager.currentType() == GameSessionStateType.COMPLETED) {
            timeoutCoordinator.cancel(result.session().getId());
            decisionTimeoutCoordinator.cancel(result.session().getId());
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
            respondError(event.userId(), Type.PLACE_STONE, event.requestId(), message);
            drainPostGameSideEffects(manager, ctx);
            return;
        }
        respondToUser(
            event.userId(),
            Type.PLACE_STONE,
            event.requestId(),
            MoveAckMessageEncoder.encode(result)
        );
        if (result.status() == MoveStatus.SUCCESS) {
            broadcastStonePlaced(result);
            if (result.turnSnapshot() != null && manager.currentType() == GameSessionStateType.TURN_WAITING) {
                scheduleTurnTimeout(result.session(), result.turnSnapshot());
            } else if (result.turnSnapshot() == null) {
                timeoutCoordinator.cancel(result.session().getId());
            }
        }
        if (manager.currentType() == GameSessionStateType.COMPLETED) {
            timeoutCoordinator.cancel(result.session().getId());
            decisionTimeoutCoordinator.cancel(result.session().getId());
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
                TurnService.TurnSnapshot snapshot =
                    turnService.snapshot(session.getTurnStore(), session.getUserIds());
                if (snapshot != null) {
                    scheduleTurnTimeout(session, snapshot);
                }
            }
            drainPostGameSideEffects(manager, ctx);
            return;
        }
        boolean waiting = manager.currentType() == GameSessionStateType.TURN_WAITING;
        if (result.timedOut()) {
            broadcastTurnTimeout(session, result);
            if (result.nextTurn() != null && waiting) {
                scheduleTurnTimeout(session, result.nextTurn());
            }
        } else if (result.currentTurn() != null && waiting) {
            scheduleTurnTimeout(session, result.currentTurn());
        }
        if (manager.currentType() == GameSessionStateType.COMPLETED) {
            timeoutCoordinator.cancel(session.getId());
            decisionTimeoutCoordinator.cancel(session.getId());
        }
        drainPostGameSideEffects(manager, ctx);
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

    private void handleDecisionTimeout(UUID sessionId) {
        Optional<GameSession> optionalSession = store.findById(sessionId);
        if (optionalSession.isEmpty()) {
            return;
        }
        GameSession session = optionalSession.get();
        GameSessionStateManager manager = store.ensureManager(session);
        long now = System.currentTimeMillis();
        DecisionTimeoutEvent event = new DecisionTimeoutEvent(now);
        manager.submit(event, ctx -> handleDecisionTimeoutCompletion(session, manager, ctx, event));
    }

    private void handleDecisionTimeoutCompletion(GameSession session,
                                                 GameSessionStateManager manager,
                                                 GameSessionStateContext ctx,
                                                 DecisionTimeoutEvent event) {
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
        respondPostGameDecision(event.userId(), event.requestId(), result);
        drainPostGameSideEffects(manager, ctx);
    }

    private void drainPostGameSideEffects(GameSessionStateManager manager,
                                          GameSessionStateContext ctx) {
        GameCompletionNotice completion = ctx.consumePendingGameCompletion();
        if (completion != null) {
            broadcastGameCompleted(completion.session());
        }
        PostGameDecisionPrompt prompt = ctx.consumePendingDecisionPrompt();
        if (prompt != null) {
            broadcastPostGamePrompt(prompt);
            scheduleDecisionTimeout(prompt.session(), prompt.deadlineAt());
        }
        PostGameDecisionUpdate update = ctx.consumePendingDecisionUpdate();
        if (update != null) {
            broadcastPostGameDecisionUpdate(update);
        }
        PostGameResolution resolution = ctx.consumePendingPostGameResolution();
        if (resolution != null) {
            handlePostGameResolution(resolution);
        }
    }

    private void handlePostGameResolution(PostGameResolution resolution) {
        GameSession session = resolution.session();
        timeoutCoordinator.cancel(session.getId());
        decisionTimeoutCoordinator.cancel(session.getId());
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
        store.save(newSession);
        NioReactorServer srv = server;
        if (srv != null) {
            broadcastJoin(srv, newSession);
        }
        broadcastRematchStarted(oldSession, newSession, participants);
        broadcastSessionTerminated(oldSession, resolution.disconnected());
        store.removeById(oldSession.getId());
    }

    private void handleSessionTermination(GameSession session, List<String> disconnected) {
        broadcastSessionTerminated(session, disconnected);
        store.removeById(session.getId());
    }

    private void scheduleDecisionTimeout(GameSession session, long deadlineAt) {
        decisionTimeoutCoordinator.schedule(session.getId(), deadlineAt, () -> handleDecisionTimeout(session.getId()));
    }

    public byte[] boardSnapshot(GameSession session) {
        return boardService.snapshot(session.getBoardStore());
    }

    public boolean isTurnExpired(GameSession session, long now) {
        return turnService.isExpired(session.getTurnStore(), now);
    }

    public TurnService.TurnSnapshot turnSnapshot(GameSession session) {
        return turnService.snapshot(session.getTurnStore(), session.getUserIds());
    }

    public Map<String, Boolean> readyStatesView(GameSession session) {
        return session.readyStatesView();
    }

    public enum MoveStatus {
        SUCCESS,
        INVALID_PLAYER,
        GAME_NOT_STARTED,
        OUT_OF_TURN,
        OUT_OF_BOUNDS,
        CELL_OCCUPIED,
        GAME_FINISHED
    }

    public record ReadyResult(GameSession session, boolean validUser, boolean stateChanged,
                              boolean allReady, boolean gameStartedNow,
                              TurnService.TurnSnapshot firstTurn, String userId) {
        public static ReadyResult invalid(GameSession session, String userId) {
            return new ReadyResult(session, false, false, false, false, null, userId);
        }
    }

    public record MoveResult(GameSession session, MoveStatus status, Stone placedAs,
                             TurnService.TurnSnapshot turnSnapshot, String userId, int x, int y) {
        public static MoveResult success(GameSession session, Stone stone, TurnService.TurnSnapshot nextTurn,
                                  String userId, int x, int y) {
            return new MoveResult(session, MoveStatus.SUCCESS, stone, nextTurn, userId, x, y);
        }

        public static MoveResult invalid(GameSession session, MoveStatus status, TurnService.TurnSnapshot snapshot,
                                  String userId, int x, int y) {
            return new MoveResult(session, status, null, snapshot, userId, x, y);
        }
    }

    public record TurnTimeoutResult(GameSession session, boolean timedOut,
                                    TurnService.TurnSnapshot currentTurn,
                                    TurnService.TurnSnapshot nextTurn,
                                    String previousPlayerId) {
        public static TurnTimeoutResult noop(GameSession session, TurnService.TurnSnapshot current) {
            return new TurnTimeoutResult(session, false, current, current, null);
        }

        public static TurnTimeoutResult timedOut(GameSession session, TurnService.TurnSnapshot current,
                                          TurnService.TurnSnapshot next, String previousPlayerId) {
            return new TurnTimeoutResult(session, true, current, next, previousPlayerId);
        }
    }

    public enum PostGameDecisionStatus {
        ACCEPTED,
        INVALID_PLAYER,
        ALREADY_DECIDED,
        TIME_WINDOW_CLOSED,
        SESSION_CLOSED,
        SESSION_NOT_FOUND,
        INVALID_PAYLOAD
    }

    public record PostGameDecisionResult(GameSession session,
                                         String userId,
                                         PostGameDecision decision,
                                         PostGameDecisionStatus status) {
        public static PostGameDecisionResult accepted(GameSession session,
                                                      String userId,
                                                      PostGameDecision decision) {
            return new PostGameDecisionResult(session, userId, decision, PostGameDecisionStatus.ACCEPTED);
        }

        public static PostGameDecisionResult rejected(GameSession session,
                                                      String userId,
                                                      PostGameDecisionStatus status) {
            return new PostGameDecisionResult(session, userId, null, status);
        }
    }

    public record PostGameDecisionPrompt(GameSession session, long deadlineAt) { }

    public record PostGameDecisionUpdate(GameSession session,
                                         Map<String, PostGameDecision> decisions,
                                         List<String> remaining) { }

    public record PostGameResolution(GameSession session,
                                     ResolutionType type,
                                     List<String> rematchParticipants,
                                     List<String> disconnected) {
        public enum ResolutionType {
            REMATCH,
            TERMINATE
        }

        public static PostGameResolution rematch(GameSession session,
                                                 List<String> participants,
                                                 List<String> disconnected) {
            return new PostGameResolution(session,
                ResolutionType.REMATCH,
                List.copyOf(participants),
                List.copyOf(disconnected));
        }

        public static PostGameResolution terminate(GameSession session,
                                                   List<String> disconnected) {
            return new PostGameResolution(session,
                ResolutionType.TERMINATE,
                List.of(),
                List.copyOf(disconnected));
        }
    }

    public record GameCompletionNotice(GameSession session) { }
}
