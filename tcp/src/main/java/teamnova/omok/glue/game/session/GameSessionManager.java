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
import teamnova.omok.glue.game.session.interfaces.manager.GameSessionEventProcessor;
import teamnova.omok.glue.game.session.interfaces.GameSessionMessenger;
import teamnova.omok.glue.game.session.interfaces.manager.GameSessionOperations;
import teamnova.omok.glue.game.session.interfaces.GameSessionRuntime;
import teamnova.omok.glue.game.session.interfaces.manager.TurnTimeoutScheduler;
import teamnova.omok.glue.game.session.model.GameSession;
import teamnova.omok.glue.game.session.model.PostGameDecision;
import teamnova.omok.glue.game.session.model.vo.GameSessionId;
import teamnova.omok.glue.game.session.repository.GameStateHubRegistry;
import teamnova.omok.glue.game.session.repository.InMemoryGameSessionRepository;
import teamnova.omok.glue.game.session.services.*;
import teamnova.omok.glue.game.session.services.coordinator.DecisionTimeoutCoordinator;
import teamnova.omok.glue.game.session.services.coordinator.TurnTimeoutCoordinator;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContextService;
import teamnova.omok.glue.rule.RuleManager;
import teamnova.omok.modules.matching.models.MatchGroup;

/**
 * Central facade coordinating game session lifecycle and dependencies.
 */
public final class GameSessionManager implements Closeable,
                                                 GameSessionOperations,
                                                 GameSessionEventProcessor,
                                                 TurnTimeoutScheduler.TurnTimeoutConsumer {
    private static final long DEFAULT_TICK_MILLIS = 20L;

    private static GameSessionManager INSTANCE;

    public static GameSessionManager Init(RuleManager ruleManager, ClientSessionManager clientSessionManager) {
        INSTANCE = new GameSessionManager(ruleManager, clientSessionManager);
        return INSTANCE;
    }

    public static GameSessionManager getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("GameSessionManager not initialized");
        }
        return INSTANCE;
    }

    private final GameSessionRuntime runtime;
    private final GameSessionDependencies dependencies;
    private final SessionEventService eventService;
    private final ScheduledExecutorService scheduler;
    private final AtomicBoolean ticking = new AtomicBoolean(false);

    private GameSessionManager(RuleManager ruleManager, ClientSessionManager clientSessionManager) {
        Objects.requireNonNull(ruleManager, "ruleManager");
        Objects.requireNonNull(clientSessionManager, "clientSessionManager");

        InMemoryGameSessionRepository repository = new InMemoryGameSessionRepository();

        BoardService boardService = new BoardService();
        TurnService turnService = new TurnService(GameSession.TURN_DURATION_MILLIS);
        ScoreService scoreService = new ScoreService();
        RuleService ruleService = RuleService.Init();

        TurnTimeoutCoordinator turnTimeoutScheduler = new TurnTimeoutCoordinator();
        DecisionTimeoutCoordinator decisionTimeoutScheduler = new DecisionTimeoutCoordinator();


        GameSessionStateContextService contextService = new GameSessionStateContextService();
        GameSessionMessenger messenger = clientSessionManager.gamePublisher();
        this.runtime = new GameStateHubRegistry(repository, boardService, turnService, scoreService, contextService, messenger, turnTimeoutScheduler, decisionTimeoutScheduler);
        this.dependencies = new GameSessionDependencies(
            repository,
            runtime,
            turnService,
            messenger,
            turnTimeoutScheduler,
            decisionTimeoutScheduler,
            ruleManager,
            contextService
        );
        this.eventService = new SessionEventService(dependencies);
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setName("game-session-ticker");
            t.setDaemon(true);
            return t;
        });
    }

    public void start(NioReactorServer server) {
        Objects.requireNonNull(server, "server");
        attachServer(server);
        startTicker();
    }

    public void startTicker() {
        if (ticking.compareAndSet(false, true)) {
            scheduler.scheduleAtFixedRate(() -> {
                try {
                    runtime.tick(System.currentTimeMillis());
                } catch (Throwable t) {
                    System.err.println("[SESSION][ticker] Uncaught error in tick: " + t);
                    t.printStackTrace();
                }
            },
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
        return findByUser(userId);
    }

    public void leaveSession(String userId) {
        leaveByUser(userId);
    }

    @Override
    public void handleClientDisconnected(String userId) {
        GameSessionLifecycleService.handleClientDisconnected(dependencies, eventService, userId);
    }

    @Override
    public boolean submitReady(String userId, long requestId) {
        return eventService.submitReady(userId, requestId);
    }

    @Override
    public boolean submitMove(String userId, long requestId, int x, int y) {
        return eventService.submitMove(userId, requestId, x, y);
    }

    @Override
    public boolean submitPostGameDecision(String userId, long requestId, PostGameDecision decision) {
        return eventService.submitPostGameDecision(userId, requestId, decision);
    }

    @Override
    public void cancelAllTimers(GameSessionId sessionId) {
        eventService.cancelAllTimers(sessionId);
    }

    @Override
    public void skipTurnForDisconnected(GameSession session, String userId, int expectedTurnNumber) {
        eventService.skipTurnForDisconnected(session, userId, expectedTurnNumber);
    }

    @Override
    public void createFromGroup(NioReactorServer server, MatchGroup group) {
        Objects.requireNonNull(server, "server");
        GameSessionCreationService.createFromGroup(dependencies, group);
    }

    @Override
    public void attachServer(NioReactorServer server) {
        Objects.requireNonNull(server, "server");
    }

    @Override
    public Optional<GameSession> findByUser(String userId) {
        Objects.requireNonNull(userId, "userId");
        return dependencies.repository().findByUserId(userId);
    }

    @Override
    public void leaveByUser(String userId) {
        GameSessionLifecycleService.leaveByUser(dependencies, eventService, userId);
    }

    @Override
    public void onTimeout(GameSessionId sessionId, int expectedTurnNumber) {
        eventService.handleScheduledTimeout(sessionId, expectedTurnNumber);
    }

    @Override
    public void close() {
        stopTicker();
    }
}
