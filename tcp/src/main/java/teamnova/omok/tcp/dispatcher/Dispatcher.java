package teamnova.omok.tcp.dispatcher;

import java.io.Closeable;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import teamnova.omok.tcp.event.WorkerEvent;
import teamnova.omok.tcp.nio.NioReactorServer;

/**
 * Dispatcher that hands worker events to a thread pool.
 */
public final class Dispatcher implements Closeable {
    private final ExecutorService executor;
    private final NioReactorServer server;

    public Dispatcher(int poolSize, NioReactorServer server) {
        int size = Math.max(1, poolSize);
        this.executor = Executors.newFixedThreadPool(size, new DispatcherThreadFactory());
        this.server = Objects.requireNonNull(server, "server");
    }

    public void submit(WorkerEvent event) {
        executor.execute(() -> event.execute(server));
    }

    @Override
    public void close() {
        executor.shutdownNow();
        try {
            if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            executor.shutdownNow();
        }
    }

    private static final class DispatcherThreadFactory implements ThreadFactory {
        private final AtomicInteger counter = new AtomicInteger();

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setName("dispatcher-" + counter.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        }
    }
}
