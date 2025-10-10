package teamnova.omok.service;

import teamnova.omok.nio.NioReactorServer;
import teamnova.omok.store.GameSession;
import teamnova.omok.store.InGameSessionStore;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Simple service container (singleton) holding core services and scheduling matching.
 */
public class ServiceContainer {
    private static final ServiceContainer INSTANCE = new ServiceContainer();

    private final DotenvService dotenvService;
    private final MysqlService mysqlService;
    private final BoardService boardService;
    private final TurnService turnService;
    private final OutcomeService outcomeService;
    private final MatchingService matchingService;
    private final InGameSessionStore inGameSessionStore;
    private final InGameSessionService inGameSessionService;

    private final ScheduledExecutorService matchScheduler;
    private final ScheduledExecutorService sessionScheduler;
    private volatile boolean started;

    private ServiceContainer() {
        // .env is located at project root's parent (same as previous usage in DefaultHandlerRegistry)
        String basePath = System.getProperty("user.dir") + "/..";
        this.dotenvService = new DotenvService(basePath);
        this.mysqlService = new MysqlService(dotenvService);
        this.boardService = new BoardService();
        this.turnService = new TurnService(GameSession.TURN_DURATION_MILLIS);
        this.outcomeService = new OutcomeService(boardService);
        this.matchingService = new MatchingService();
        this.inGameSessionStore = new InGameSessionStore(boardService, turnService, outcomeService);
        this.inGameSessionService = new InGameSessionService(inGameSessionStore, boardService, turnService, outcomeService);
        this.matchScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setName("match-scheduler");
            t.setDaemon(true);
            return t;
        });
        this.sessionScheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setName("session-scheduler");
            t.setDaemon(true);
            return t;
        });
        this.started = false;
    }

    public static ServiceContainer getInstance() {
        return INSTANCE;
    }

    public synchronized void start(NioReactorServer server) {
        if (started) return;
        Objects.requireNonNull(server, "server");
        inGameSessionService.attachServer(server);
        sessionScheduler.scheduleAtFixedRate(() -> {
            try {
                long now = System.currentTimeMillis();
                inGameSessionStore.updateSessions(now);
            } catch (Exception ignored) {
                // keep loop running even if one tick fails
            }
        }, 0, 20, TimeUnit.MILLISECONDS);
        // Schedule tryMatch every 500 ms
        matchScheduler.scheduleAtFixedRate(() -> {
            try {
                MatchingService.Result result = matchingService.tryMatch();
                if (result instanceof MatchingService.Result.Success s) {
                    inGameSessionService.createFromGroup(server, s.group());
                }
            } catch (Exception ignored) {
                // swallow exceptions to keep scheduler running
            }
        }, 500, 500, TimeUnit.MILLISECONDS);
        started = true;
    }

    public DotenvService getDotenvService() { return dotenvService; }
    public MysqlService getMysqlService() { return mysqlService; }

    public MatchingService getMatchingService() {
        return matchingService;
    }

    public BoardService getBoardService() {
        return boardService;
    }

    public TurnService getTurnService() {
        return turnService;
    }

    public OutcomeService getOutcomeService() {
        return outcomeService;
    }

    public InGameSessionService getInGameSessionService() {
        return inGameSessionService;
    }

    public InGameSessionStore getInGameSessionStore() {
        return inGameSessionStore;
    }
}
