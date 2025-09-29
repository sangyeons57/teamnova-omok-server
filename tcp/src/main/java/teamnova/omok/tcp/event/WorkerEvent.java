package teamnova.omok.tcp.event;

import teamnova.omok.tcp.nio.NioReactorServer;

/**
 * Logical event dispatched to worker threads.
 */
@FunctionalInterface
public interface WorkerEvent {
    void execute(NioReactorServer server);
}
