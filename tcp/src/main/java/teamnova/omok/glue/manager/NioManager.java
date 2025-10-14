package teamnova.omok.glue.manager;

import java.io.Closeable;
import java.io.IOException;
import java.util.Objects;
import teamnova.omok.core.nio.NioReactorServer;
import teamnova.omok.glue.handler.register.HandlerRegistry;

/**
 * Coordinates the lifecycle of the NIO reactor server.
 */
public final class NioManager implements Closeable {
    private final NioReactorServer server;
    private final int port;

    public NioManager(int port, int workerCount, HandlerRegistry registry) throws IOException {
        this.port = port;
        this.server = new NioReactorServer(
            port,
            workerCount,
            Objects.requireNonNull(registry, "registry")
        );
    }

    public int getPort() {
        return port;
    }

    public NioReactorServer getServer() {
        return server;
    }

    /**
     * Runs the selector loop until {@link #close()} is invoked.
     */
    public void run() {
        server.start();
    }

    @Override
    public void close() {
        server.close();
    }
}
