package teamnova.omok.glue.service;

import teamnova.omok.core.nio.NioReactorServer;
import teamnova.omok.glue.store.GameSession;
import teamnova.omok.glue.store.InGameSessionStore;
import teamnova.omok.modules.matching.MatchingGateway;

import java.util.Objects;

/**
 * Simple service container (singleton) holding core services.
 */
public class ServiceManager {
    private static ServiceManager INSTANCE;

    public static ServiceManager Init() {
        INSTANCE = new ServiceManager();
        return INSTANCE;
    }
    public static ServiceManager getInstance() {
        if( INSTANCE == null) {
            throw new IllegalStateException("ServiceManager not initialized");
        }
        return INSTANCE;
    }

    private final DotenvService dotenvService;
    private final MysqlService mysqlService;
    private final ScoreService scoreService;
    private final BoardService boardService;
    private final TurnService turnService;
    private final InGameSessionStore inGameSessionStore;
    private final InGameSessionService inGameSessionService;
    private final teamnova.omok.glue.rule.RuleManager ruleManager;

    private ServiceManager() {
        // .env is located at project root's parent (same as previous usage in DefaultHandlerRegistry)
        String basePath = System.getProperty("user.dir") + "/..";
        this.dotenvService = new DotenvService(basePath);
        this.mysqlService = new MysqlService(dotenvService);
        this.scoreService = new ScoreService(mysqlService);
        this.boardService = new BoardService();
        this.turnService = new TurnService(GameSession.TURN_DURATION_MILLIS);
        this.inGameSessionStore = new InGameSessionStore(boardService, turnService, scoreService);
        this.ruleManager = new teamnova.omok.glue.rule.RuleManager(mysqlService);
        this.inGameSessionService = new InGameSessionService(
            inGameSessionStore,
            turnService,
            scoreService,
            ruleManager
        );
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


    public BoardService getBoardService() {
        return boardService;
    }

    public TurnService getTurnService() {
        return turnService;
    }

    public teamnova.omok.glue.rule.RuleManager getRuleManager() {
        return ruleManager;
    }

    public InGameSessionService getInGameSessionService() {
        return inGameSessionService;
    }

    public InGameSessionStore getInGameSessionStore() {
        return inGameSessionStore;
    }
}
