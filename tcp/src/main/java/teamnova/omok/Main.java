package teamnova.omok;

import java.io.IOException;
import teamnova.omok.handler.register.DefaultHandlerRegistry;
import teamnova.omok.nio.NioReactorServer;

public final class Main {
    private static final int DEFAULT_PORT = 15015;

    public static void main(String[] args) {
        int port = parsePort(args);
        try (NioReactorServer server = new NioReactorServer(port,
                Runtime.getRuntime().availableProcessors(),
                new DefaultHandlerRegistry())) {
            System.out.printf("[NIO] Reactor server listening on port %d%n", port);
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
