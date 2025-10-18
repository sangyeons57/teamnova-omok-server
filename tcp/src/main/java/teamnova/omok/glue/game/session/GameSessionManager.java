package teamnova.omok.glue.game.session;

import java.io.Closeable;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import teamnova.omok.core.nio.NioReactorServer;
import teamnova.omok.glue.client.session.ClientSessionManager;
import teamnova.omok.glue.game.session.interfaces.GameBoardService;
import teamnova.omok.glue.game.session.interfaces.GameScoreService;
import teamnova.omok.glue.game.session.interfaces.GameSessionMessenger;
import teamnova.omok.glue.game.session.interfaces.GameSessionOperations;
import teamnova.omok.glue.game.session.interfaces.GameSessionRepository;
import teamnova.omok.glue.game.session.interfaces.GameSessionRuntime;
import teamnova.omok.glue.game.session.interfaces.GameTurnService;
import teamnova.omok.glue.game.session.interfaces.GameSessionEventProcessor;
import teamnova.omok.glue.game.session.interfaces.TurnTimeoutScheduler;
import teamnova.omok.glue.game.session.interfaces.DecisionTimeoutScheduler;
import teamnova.omok.glue.game.session.model.GameSession;
import teamnova.omok.glue.game.session.model.PostGameDecision;
import teamnova.omok.glue.game.session.services.BoardService;
import teamnova.omok.glue.game.session.services.InGameSessionService;
import teamnova.omok.glue.game.session.repository.InMemoryGameSessionRepository;
import teamnova.omok.glue.game.session.repository.GameStateHubRegistry;
import teamnova.omok.glue.game.session.services.ScoreService;
import teamnova.omok.glue.game.session.services.SessionEventService;
import teamnova.omok.glue.game.session.services.TurnService;
import teamnova.omok.glue.game.session.services.coordinator.DecisionTimeoutCoordinator;
import teamnova.omok.glue.game.session.services.coordinator.TurnTimeoutCoordinator;
import teamnova.omok.glue.rule.RuleManager;
import teamnova.omok.modules.matching.models.MatchGroup;

/**
 * Central facade coordinating game session lifecycle and dependencies.
 */
public final class GameSessionManager implements Closeable {
    private static final long DEFAULT_TICK_MILLIS = 20L;

    private static GameSessionManager INSTANCE;

    public static GameSessionManager Init(RuleManager ruleManager) {
        INSTANCE = new GameSessionManager(ruleManager);
        return INSTANCE;
    }

    public static GameSessionManager getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("GameSessionManager not initialized");
        }
        return INSTANCE;
    }

    private final GameBoardService boardService;
    private final GameTurnService turnService;
    private final GameScoreService scoreService;
    private final GameSessionRepository repository;
    private final GameSessionRuntime runtime;
    private final DecisionTimeoutScheduler decisionTimeoutScheduler;
    private final TurnTimeoutScheduler turnTimeoutScheduler;
    private final GameSessionMessenger messenger;
    private final GameSessionEventProcessor eventProcessor;
    private final GameSessionOperations operations;
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean ticking = new AtomicBoolean(false);

    private GameSessionManager(RuleManager ruleManager) {
        Objects.requireNonNull(ruleManager, "ruleManager");
        this.boardService = new BoardService();
        this.turnService = new TurnService(GameSession.TURN_DURATION_MILLIS);
        this.scoreService = new ScoreService();
        this.repository = new InMemoryGameSessionRepository();
        this.runtime = new GameStateHubRegistry(boardService, turnService, scoreService);
        this.turnTimeoutScheduler = new TurnTimeoutCoordinator();
        this.decisionTimeoutScheduler = new DecisionTimeoutCoordinator();
        this.messenger = ClientSessionManager.getInstance().gamePublisher();
        this.eventProcessor = new SessionEventService(
            repository,
            runtime,
            turnService,
            messenger,
            turnTimeoutScheduler,
            decisionTimeoutScheduler,
            ruleManager
        );
        this.operations = new InGameSessionService(
            repository,
            runtime,
            turnService,
            messenger,
            eventProcessor,
            ruleManager
        );
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setName("game-session-ticker");
            t.setDaemon(true);
            return t;
        });
    }

    public void start(NioReactorServer server) {
        Objects.requireNonNull(server, "server");
        operations.attachServer(server);
        startTicker();
    }

    public void startTicker() {
        if (ticking.compareAndSet(false, true)) {
            scheduler.scheduleAtFixedRate(() ->
                runtime.tick(System.currentTimeMillis()),
                0L,
                DEFAULT_TICK_MILLIS,
                TimeUnit.MILLISECONDS
            );
        }
    }

    public void stopTicker() {
        if (ticking.compareAndSet(true, false)) {
            scheduler.shutdownNow();
        }
    }

    public Optional<GameSession> findSession(String userId) {
        return operations.findByUser(userId);
    }

    public void leaveSession(String userId) {
        operations.leaveByUser(userId);
    }

    public void handleClientDisconnected(String userId) {
        operations.handleClientDisconnected(userId);
    }

    public boolean submitReady(String userId, long requestId) {
        return operations.submitReady(userId, requestId);
    }

    public boolean submitMove(String userId, long requestId, int x, int y) {
        return operations.submitMove(userId, requestId, x, y);
    }

    public boolean submitPostGameDecision(String userId, long requestId, PostGameDecision decision) {
        return operations.submitPostGameDecision(userId, requestId, decision);
    }

    public void createFromGroup(NioReactorServer server, MatchGroup group) {
        operations.createFromGroup(server, group);
    }

    public GameSessionMessenger messenger() { return messenger; }
    public GameSessionEventProcessor eventProcessor() { return eventProcessor; }
    public GameBoardService boardService() { return boardService; }
    public GameTurnService turnService() { return turnService; }
    public GameScoreService scoreService() { return scoreService; }

    @Override
    public void close() {
        stopTicker();
    }
}
