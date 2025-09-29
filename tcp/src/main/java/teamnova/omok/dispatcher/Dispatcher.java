package teamnova.omok.dispatcher;

import java.io.Closeable;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import teamnova.omok.handler.FrameHandler;
import teamnova.omok.handler.HandlerProvider;
import teamnova.omok.nio.ClientSession;
import teamnova.omok.nio.FramedMessage;
import teamnova.omok.nio.NioReactorServer;

/**
 * Dispatcher that delegates frames to registered handlers using a worker pool.
 */
public final class Dispatcher implements Closeable {
    private final ExecutorService executor;
    private final NioReactorServer server;
    private final Map<Integer, HandlerProvider> handlers = new ConcurrentHashMap<>();

    public Dispatcher(int poolSize, NioReactorServer server) {
        int size = Math.max(1, poolSize);
        this.executor = Executors.newFixedThreadPool(size, new DispatcherThreadFactory());
        this.server = Objects.requireNonNull(server, "server");
    }

    public void register(int type, HandlerProvider provider) {
        Objects.requireNonNull(provider, "provider");
        if (type < 0 || type > 255) {
            throw new IllegalArgumentException("type must be within 0-255 range");
        }
        HandlerProvider previous = handlers.putIfAbsent(type, provider);
        if (previous != null) {
            throw new IllegalStateException("Handler already registered for type " + type);
        }
    }

    public void dispatch(ClientSession session, FramedMessage frame) {
        int type = Byte.toUnsignedInt(frame.type());
        HandlerProvider provider = handlers.get(type);
        if (provider == null) {
            System.err.println("No handler registered for type " + type + ", dropping frame");
            return;
        }
        executor.execute(() -> {
            FrameHandler handler = provider.acquire();
            try {
                handler.handle(server, session, frame);
            } catch (Exception e) {
                System.err.println("Handler failure for type " + type + ": " + e.getMessage());
            }
        });
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
