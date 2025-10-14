package teamnova.omok;

import java.io.IOException;
import teamnova.omok.handler.register.DefaultHandlerRegistry;
import teamnova.omok.infra.InfraContainer;
import teamnova.omok.nio.NioReactorServer;
import teamnova.omok.service.ServiceContainer;

public final class Main {
    private static final int DEFAULT_PORT = 15015;

    public static void main(String[] args) {
        CompositionRoot compositionRoot = new CompositionRoot();
        int port = parsePort(args);
        try (NioReactorServer server = new NioReactorServer(port,
                Runtime.getRuntime().availableProcessors(),
                compositionRoot.handlerRegistry())) {
            System.out.printf("[NIO] Reactor server listening on port %d%n", port);
            // Start service container scheduler

            compositionRoot.publisher().getMessenger().attachServer(server);


            ServiceContainer.getInstance().start(server);

            server.start();
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
