package teamnova.omok.glue.service;

import teamnova.omok.core.nio.NioReactorServer;
import teamnova.omok.glue.data.DotenvService;
import teamnova.omok.glue.data.MysqlService;
import teamnova.omok.glue.rule.RuleManager;
import teamnova.omok.glue.rule.RuleRegistry;
import teamnova.omok.glue.store.GameSession;
import teamnova.omok.glue.store.InGameSessionStore;

import java.util.Objects;

/**
 * Simple service container (singleton) holding core services.
 */
public class ServiceManager {
    private static ServiceManager INSTANCE;

    public static ServiceManager Init(RuleManager ruleManager) {
        INSTANCE = new ServiceManager(ruleManager);
        return INSTANCE;
    }
    public static ServiceManager getInstance() {
        if( INSTANCE == null) {
            throw new IllegalStateException("ServiceManager not initialized");
        }
        return INSTANCE;
    }

    private final ScoreService scoreService;
    private final BoardService boardService;
    private final TurnService turnService;
    private final InGameSessionStore inGameSessionStore;
    private final InGameSessionService inGameSessionService;
    private final RuleManager ruleManager;

    private ServiceManager(RuleManager ruleManager) {
        // .env is located at project root's parent (same as previous usage in DefaultHandlerRegistry)
        this.ruleManager = ruleManager;
        this.scoreService = new ScoreService();
        this.boardService = new BoardService();
        this.turnService = new TurnService(GameSession.TURN_DURATION_MILLIS);
        this.inGameSessionStore = new InGameSessionStore(boardService, turnService, scoreService);
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
