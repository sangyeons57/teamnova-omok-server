package teamnova.omok.glue.manager;

import java.io.Closeable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import teamnova.omok.glue.client.session.ClientSessionManager;

/**
 * Advances client state machines for authenticated sessions.
 */
public final class UserSessionManager implements Closeable {
    private static final long DEFAULT_INTERVAL_MILLIS = 50L;
    private static UserSessionManager INSTANCE;

    public static UserSessionManager Init() {
        INSTANCE = new UserSessionManager(DEFAULT_INTERVAL_MILLIS);
        return INSTANCE;
    }

    public static UserSessionManager getInstance() {
        return INSTANCE;
    }


    private final ScheduledExecutorService scheduler;
    private final long intervalMillis;
    private final AtomicBoolean running = new AtomicBoolean(false);

    private UserSessionManager(long intervalMillis) {
        this.intervalMillis = intervalMillis;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setName("user-session-manager");
            t.setDaemon(true);
            return t;
        });
    }

    public void start() {
        if (running.compareAndSet(false, true)) {
            scheduler.scheduleAtFixedRate(this::tick, intervalMillis, intervalMillis, TimeUnit.MILLISECONDS);
        }
    }

    private void tick() {
        long now = System.currentTimeMillis();
        ClientSessionManager.getInstance().forEachAuthenticated(session -> {
            try {
                session.processLifecycle(now);
            } catch (Exception ignored) {
                // keep iterating despite per-session errors
            }
        });
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
