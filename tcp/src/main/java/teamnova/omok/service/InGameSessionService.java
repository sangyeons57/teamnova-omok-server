package teamnova.omok.service;

import teamnova.omok.handler.register.Type;
import teamnova.omok.game.PlayerResult;
import teamnova.omok.message.encoder.ErrorMessageEncoder;
import teamnova.omok.message.encoder.GameSessionStartedMessageEncoder;
import teamnova.omok.message.encoder.JoinSessionMessageEncoder;
import teamnova.omok.message.encoder.MoveAckMessageEncoder;
import teamnova.omok.message.encoder.ReadyStateMessageEncoder;
import teamnova.omok.message.encoder.StonePlacedMessageEncoder;
import teamnova.omok.message.encoder.TurnTimeoutMessageEncoder;
import teamnova.omok.nio.ClientSession;
import teamnova.omok.nio.NioReactorServer;
import teamnova.omok.state.game.event.MoveEvent;
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
            session.lock().lock();
            try {
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
            return;
        }
        if (!result.validUser()) {
            respondError(event.userId(), Type.READY_IN_GAME_SESSION, event.requestId(), "INVALID_PLAYER");
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
        }
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
        }
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
        }
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
}
