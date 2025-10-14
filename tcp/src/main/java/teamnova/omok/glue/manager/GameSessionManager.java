package teamnova.omok.glue.manager;

import java.io.Closeable;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import teamnova.omok.glue.store.InGameSessionStore;

/**
 * Periodically advances game session state machines.
 */
public final class GameSessionManager implements Closeable {
    private static final long DEFAULT_TICK_MILLIS = 20L;

    private final InGameSessionStore store;
    private final ScheduledExecutorService scheduler;
    private final long tickMillis;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public GameSessionManager(InGameSessionStore store) {
        this(store, DEFAULT_TICK_MILLIS);
    }

    public GameSessionManager(InGameSessionStore store, long tickMillis) {
        this.store = Objects.requireNonNull(store, "store");
        this.tickMillis = tickMillis;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setName("game-session-manager");
            t.setDaemon(true);
            return t;
        });
    }

    public void start() {
        if (running.compareAndSet(false, true)) {
            scheduler.scheduleAtFixedRate(this::runUpdate, 0L, tickMillis, TimeUnit.MILLISECONDS);
        }
    }

    private void runUpdate() {
        try {
            store.updateSessions(System.currentTimeMillis());
        } catch (Exception ignored) {
            // keep loop running even if one tick fails
        }
    }

    public void stop() {
        if (running.compareAndSet(true, false)) {
            scheduler.shutdownNow();
        }
    }

    @Override
    public void close() {
        stop();
    }
}
