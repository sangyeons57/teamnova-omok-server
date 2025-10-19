package teamnova.omok;

import java.io.IOException;

import teamnova.omok.glue.client.session.ClientSessionManager;
import teamnova.omok.glue.game.session.GameSessionManager;
import teamnova.omok.glue.handler.register.DefaultHandlerRegistry;
import teamnova.omok.glue.manager.DataManager;
import teamnova.omok.glue.manager.MatchingManager;
import teamnova.omok.glue.manager.NioManager;
import teamnova.omok.glue.manager.ServerLifecycleManager;
import teamnova.omok.glue.manager.UserSessionManager;
import teamnova.omok.glue.rule.RuleManager;
import teamnova.omok.glue.rule.RuleRegistry;

public final class Main {
    private static final int DEFAULT_PORT = 15015;

    public static void main(String[] args) {
        int port = parsePort(args);
        int workerCount = Runtime.getRuntime().availableProcessors();

        DataManager dataManager = DataManager.Init();
        ClientSessionManager clientSessionManager = ClientSessionManager.Init();
        RuleManager ruleManager = RuleManager.Init(RuleRegistry.getInstance());
        GameSessionManager gameSessionManager = GameSessionManager.Init(RuleManager.getInstance());
        MatchingManager matchingManager = MatchingManager.Init(gameSessionManager);
        UserSessionManager userSessionManager = UserSessionManager.Init();

        DefaultHandlerRegistry handlerRegistry = new DefaultHandlerRegistry();
        try (NioManager nioManager = new NioManager(port, workerCount, handlerRegistry);
             ServerLifecycleManager lifecycle = new ServerLifecycleManager(
                 nioManager,
                 gameSessionManager,
                 matchingManager,
                 userSessionManager)) {
            System.out.printf("[NIO] Reactor server listening on port %d%n", port);
            lifecycle.start();
            lifecycle.runBlocking();
        } catch (IOException e) {
            System.err.println("Failed to start NIO reactor server: " + e.getMessage());
            e.printStackTrace(System.err);
        }
    }

    private static int parsePort(String[] args) {
        if (args.length == 0) {
            return DEFAULT_PORT;
        }
        try {
            return Integer.parseInt(args[0]);
        } catch (NumberFormatException ex) {
            System.err.printf("Invalid port '%s', falling back to %d%n", args[0], DEFAULT_PORT);
            return DEFAULT_PORT;
        }
    }
}
