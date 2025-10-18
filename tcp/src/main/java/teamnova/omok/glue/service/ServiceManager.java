package teamnova.omok.glue.service;

import teamnova.omok.core.nio.NioReactorServer;
import teamnova.omok.glue.game.session.GameSessionManager;
import teamnova.omok.glue.rule.RuleManager;

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

    private final RuleManager ruleManager;
    private final GameSessionManager gameSessionManager;

    private ServiceManager(RuleManager ruleManager) {
        this.ruleManager = ruleManager;
        this.gameSessionManager = GameSessionManager.Init(ruleManager);
    }

    public synchronized void start(NioReactorServer server) {
        Objects.requireNonNull(server, "server");
        gameSessionManager.start(server);
    }

    public synchronized void stop() {
        gameSessionManager.stopTicker();
    }

    public teamnova.omok.glue.rule.RuleManager getRuleManager() {
        return ruleManager;
    }

    public GameSessionManager getGameSessionManager() {
        return gameSessionManager;
    }
}
