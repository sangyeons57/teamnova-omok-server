package teamnova.omok.service;

import teamnova.omok.nio.NioReactorServer;
import teamnova.omok.application.GameSessionManager;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Simple service container (singleton) holding core services and scheduling matching.
 */
public class ServiceContainer {
    private static final ServiceContainer INSTANCE = new ServiceContainer();

    private final ScoreService scoreService;
    private final MatchingService matchingService;
    private final GameSessionManager gameSessionManager;
    private final InGameSessionService inGameSessionService;
    private final RuleService ruleService;

    private final ScheduledExecutorService matchScheduler;
    private final ScheduledExecutorService sessionScheduler;
    private volatile boolean started;

    private ServiceContainer() {
        // .env is located at project root's parent (same as previous usage in DefaultHandlerRegistry)
        this.scoreService = new ScoreService();
        this.matchingService = new MatchingService();
        this.gameSessionManager = new GameSessionManager();
        this.ruleService = new RuleService();
        this.inGameSessionService = new InGameSessionService(
                gameSessionManager,
            scoreService,
            ruleService
        );
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
                gameSessionManager.updateSessions(now);
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

    public ScoreService getScoreService() { return scoreService; }

    public MatchingService getMatchingService() {
        return matchingService;
    }

    public RuleService getRuleService() {
        return ruleService;
    }

    public InGameSessionService getInGameSessionService() {
        return inGameSessionService;
    }

    public GameSessionManager getInGameSessionStore() {
        return gameSessionManager;
    }
}
