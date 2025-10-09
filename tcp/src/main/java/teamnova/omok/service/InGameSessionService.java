package teamnova.omok.service;

import teamnova.omok.handler.register.Type;
import teamnova.omok.message.encoder.GameSessionStartedMessageEncoder;
import teamnova.omok.message.encoder.JoinSessionMessageEncoder;
import teamnova.omok.message.encoder.ReadyStateMessageEncoder;
import teamnova.omok.message.encoder.StonePlacedMessageEncoder;
import teamnova.omok.message.encoder.TurnTimeoutMessageEncoder;
import teamnova.omok.nio.ClientSession;
import teamnova.omok.nio.NioReactorServer;
import teamnova.omok.store.GameSession;
import teamnova.omok.store.InGameSessionStore;
import teamnova.omok.store.Stone;
import teamnova.omok.store.TurnStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class InGameSessionService {
    private final InGameSessionStore store;
    private final BoardService boardService;
    private final TurnService turnService;
    private final ConcurrentMap<String, ClientSession> clients = new ConcurrentHashMap<>();
    private final ScheduledExecutorService turnScheduler;
    private final ConcurrentMap<UUID, TimeoutRef> timeoutTasks = new ConcurrentHashMap<>();

    private volatile NioReactorServer server;

    public InGameSessionService(InGameSessionStore store, BoardService boardService, TurnService turnService) {
        this.store = Objects.requireNonNull(store, "store");
        this.boardService = Objects.requireNonNull(boardService, "boardService");
        this.turnService = Objects.requireNonNull(turnService, "turnService");
        this.turnScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setName("turn-timeout");
            t.setDaemon(true);
            return t;
        });
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
        store.findByUserId(userId).ifPresent(session -> cancelTimeout(session.getId()));
        store.removeByUserId(userId);
    }

    public Optional<ReadyResult> markReady(String userId) {
        Objects.requireNonNull(userId, "userId");
        Optional<GameSession> optionalSession = store.findByUserId(userId);
        if (optionalSession.isEmpty()) {
            return Optional.empty();
        }
        GameSession session = optionalSession.get();
        ReadyResult result;
        TurnService.TurnSnapshot snapshot = null;
        long now = System.currentTimeMillis();

        session.lock().lock();
        try {
            int playerIndex = session.playerIndexOf(userId);
            if (playerIndex < 0) {
                result = ReadyResult.invalid(session, userId);
            } else {
                boolean changed = session.markReady(userId);
                boolean allReady = session.allReady();
                boolean startedNow = false;
                if (allReady && !session.isGameStarted()) {
                    startedNow = true;
                    session.markGameStarted(now);
                    boardService.reset(session.getBoardStore());
                    snapshot = turnService.start(session.getTurnStore(), session.getUserIds(), now);
                } else if (session.isGameStarted()) {
                    snapshot = turnService.snapshot(session.getTurnStore(), session.getUserIds());
                }
                result = new ReadyResult(session, true, changed, allReady, startedNow, snapshot, userId);
            }
        } finally {
            session.lock().unlock();
        }

        if (!result.validUser()) {
            return Optional.of(result);
        }

        if (result.stateChanged()) {
            broadcastReady(result);
        }
        if (result.gameStartedNow() && result.firstTurn() != null) {
            broadcastGameStart(result.session(), result.firstTurn());
            scheduleTurnTimeout(result.session(), result.firstTurn());
        }
        return Optional.of(result);
    }

    public Optional<MoveResult> placeStone(String userId, int x, int y) {
        Objects.requireNonNull(userId, "userId");
        Optional<GameSession> optionalSession = store.findByUserId(userId);
        if (optionalSession.isEmpty()) {
            return Optional.empty();
        }
        GameSession session = optionalSession.get();
        MoveResult result;
        long now = System.currentTimeMillis();

        session.lock().lock();
        try {
            int playerIndex = session.playerIndexOf(userId);
            if (playerIndex < 0) {
                result = MoveResult.invalid(session, MoveStatus.INVALID_PLAYER, null, userId, x, y);
            } else if (!session.isGameStarted()) {
                result = MoveResult.invalid(session, MoveStatus.GAME_NOT_STARTED, null, userId, x, y);
            } else {
                TurnStore turnStore = session.getTurnStore();
                TurnService.TurnSnapshot currentSnapshot = turnService.snapshot(turnStore, session.getUserIds());
                if (!boardService.isWithinBounds(session.getBoardStore(), x, y)) {
                    result = MoveResult.invalid(session, MoveStatus.OUT_OF_BOUNDS, currentSnapshot, userId, x, y);
                } else {
                    Integer currentIndex = turnService.currentPlayerIndex(turnStore);
                    if (currentIndex == null || currentIndex != playerIndex) {
                        result = MoveResult.invalid(session, MoveStatus.OUT_OF_TURN, currentSnapshot, userId, x, y);
                    } else if (!boardService.isEmpty(session.getBoardStore(), x, y)) {
                        result = MoveResult.invalid(session, MoveStatus.CELL_OCCUPIED, currentSnapshot, userId, x, y);
                    } else {
                        Stone stone = Stone.fromPlayerOrder(playerIndex);
                        boardService.setStone(session.getBoardStore(), x, y, stone);
                        TurnService.TurnSnapshot nextTurn = turnService.advance(turnStore, session.getUserIds(), now);
                        result = MoveResult.success(session, stone, nextTurn, userId, x, y);
                    }
                }
            }
        } finally {
            session.lock().unlock();
        }

        if (result.status() == MoveStatus.SUCCESS) {
            broadcastStonePlaced(result);
            if (result.turnSnapshot() != null) {
                scheduleTurnTimeout(result.session(), result.turnSnapshot());
            }
        }
        return Optional.of(result);
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
        if (turnSnapshot == null || turnSnapshot.turnEndAt() <= 0) {
            cancelTimeout(session.getId());
            return;
        }
        long now = System.currentTimeMillis();
        long delay = Math.max(0L, turnSnapshot.turnEndAt() - now);
        UUID sessionId = session.getId();
        timeoutTasks.compute(sessionId, (id, previous) -> {
            if (previous != null) {
                previous.future.cancel(false);
            }
            ScheduledFuture<?> future = turnScheduler.schedule(
                () -> onTurnTimeout(sessionId, turnSnapshot.turnNumber()),
                delay,
                TimeUnit.MILLISECONDS
            );
            return new TimeoutRef(future, turnSnapshot.turnNumber());
        });
    }

    private void onTurnTimeout(UUID sessionId, int expectedTurnNumber) {
        TimeoutRef ref = timeoutTasks.get(sessionId);
        if (ref == null || ref.turnNumber != expectedTurnNumber) {
            return;
        }
        if (!timeoutTasks.remove(sessionId, ref)) {
            return;
        }
        Optional<GameSession> optionalSession = store.findById(sessionId);
        if (optionalSession.isEmpty()) {
            return;
        }
        GameSession session = optionalSession.get();
        long now = System.currentTimeMillis();
        TurnTimeoutResult result = handleTurnTimeout(session, expectedTurnNumber, now);
        if (result.timedOut()) {
            broadcastTurnTimeout(session, result);
            if (result.nextTurn() != null) {
                scheduleTurnTimeout(session, result.nextTurn());
            }
        } else if (result.currentTurn() != null) {
            scheduleTurnTimeout(session, result.currentTurn());
        }
    }

    private TurnTimeoutResult handleTurnTimeout(GameSession session, int expectedTurnNumber, long now) {
        session.lock().lock();
        try {
            if (!session.isGameStarted()) {
                return TurnTimeoutResult.noop(session, null);
            }
            TurnService.TurnSnapshot current = turnService.snapshot(session.getTurnStore(), session.getUserIds());
            if (current.turnNumber() != expectedTurnNumber) {
                return TurnTimeoutResult.noop(session, current);
            }
            if (!turnService.isExpired(session.getTurnStore(), now)) {
                return TurnTimeoutResult.noop(session, current);
            }
            String previousPlayerId = current.currentPlayerId();
            TurnService.TurnSnapshot next = turnService.advance(session.getTurnStore(), session.getUserIds(), now);
            return TurnTimeoutResult.timedOut(session, current, next, previousPlayerId);
        } finally {
            session.lock().unlock();
        }
    }

    private void cancelTimeout(UUID sessionId) {
        TimeoutRef ref = timeoutTasks.remove(sessionId);
        if (ref != null) {
            ref.future.cancel(false);
        }
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

    private record TimeoutRef(ScheduledFuture<?> future, int turnNumber) { }

    public enum MoveStatus {
        SUCCESS,
        INVALID_PLAYER,
        GAME_NOT_STARTED,
        OUT_OF_TURN,
        OUT_OF_BOUNDS,
        CELL_OCCUPIED
    }

    public record ReadyResult(GameSession session, boolean validUser, boolean stateChanged,
                              boolean allReady, boolean gameStartedNow,
                              TurnService.TurnSnapshot firstTurn, String userId) {
        static ReadyResult invalid(GameSession session, String userId) {
            return new ReadyResult(session, false, false, false, false, null, userId);
        }
    }

    public record MoveResult(GameSession session, MoveStatus status, Stone placedAs,
                             TurnService.TurnSnapshot turnSnapshot, String userId, int x, int y) {
        static MoveResult success(GameSession session, Stone stone, TurnService.TurnSnapshot nextTurn,
                                  String userId, int x, int y) {
            return new MoveResult(session, MoveStatus.SUCCESS, stone, nextTurn, userId, x, y);
        }

        static MoveResult invalid(GameSession session, MoveStatus status, TurnService.TurnSnapshot snapshot,
                                  String userId, int x, int y) {
            return new MoveResult(session, status, null, snapshot, userId, x, y);
        }
    }

    public record TurnTimeoutResult(GameSession session, boolean timedOut,
                                    TurnService.TurnSnapshot currentTurn,
                                    TurnService.TurnSnapshot nextTurn,
                                    String previousPlayerId) {
        static TurnTimeoutResult noop(GameSession session, TurnService.TurnSnapshot current) {
            return new TurnTimeoutResult(session, false, current, current, null);
        }

        static TurnTimeoutResult timedOut(GameSession session, TurnService.TurnSnapshot current,
                                          TurnService.TurnSnapshot next, String previousPlayerId) {
            return new TurnTimeoutResult(session, true, current, next, previousPlayerId);
        }
    }
}
