package teamnova.omok.glue.service;

import teamnova.omok.core.nio.NioReactorServer;
import teamnova.omok.glue.store.GameSession;
import teamnova.omok.glue.store.InGameSessionStore;

import java.util.Objects;

/**
 * Simple service container (singleton) holding core services.
 */
public class ServiceContainer {
    private static final ServiceContainer INSTANCE = new ServiceContainer();

    private final DotenvService dotenvService;
    private final MysqlService mysqlService;
    private final ScoreService scoreService;
    private final BoardService boardService;
    private final TurnService turnService;
    private final OutcomeService outcomeService;
    private final MatchingService matchingService;
    private final InGameSessionStore inGameSessionStore;
    private final InGameSessionService inGameSessionService;
    private final RuleService ruleService;

    private ServiceContainer() {
        // .env is located at project root's parent (same as previous usage in DefaultHandlerRegistry)
        String basePath = System.getProperty("user.dir") + "/..";
        this.dotenvService = new DotenvService(basePath);
        this.mysqlService = new MysqlService(dotenvService);
        this.scoreService = new ScoreService(mysqlService);
        this.boardService = new BoardService();
        this.turnService = new TurnService(GameSession.TURN_DURATION_MILLIS);
        this.outcomeService = new OutcomeService(boardService);
        this.matchingService = new MatchingService();
        this.inGameSessionStore = new InGameSessionStore(boardService, turnService, outcomeService);
        this.ruleService = new RuleService(mysqlService);
        this.inGameSessionService = new InGameSessionService(
            inGameSessionStore,
            turnService,
            outcomeService,
            scoreService,
            ruleService
        );
    }

    public static ServiceContainer getInstance() {
        return INSTANCE;
    }

    public synchronized void start(NioReactorServer server) {
        Objects.requireNonNull(server, "server");
        inGameSessionService.attachServer(server);
    }

    public synchronized void stop() {
        // no-op for now; retained for symmetry with lifecycle manager
    }

    public DotenvService getDotenvService() { return dotenvService; }
    public MysqlService getMysqlService() { return mysqlService; }
    public ScoreService getScoreService() { return scoreService; }

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

    public RuleService getRuleService() {
        return ruleService;
    }

    public InGameSessionService getInGameSessionService() {
        return inGameSessionService;
    }

    public InGameSessionStore getInGameSessionStore() {
        return inGameSessionStore;
    }
}
