package teamnova.omok.glue.manager;

import java.io.Closeable;
import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import teamnova.omok.core.nio.NioReactorServer;
import teamnova.omok.glue.service.InGameSessionService;
import teamnova.omok.glue.service.MatchingService;

/**
 * Periodically attempts to match waiting players and spin up game sessions.
 */
public final class MatchingManager implements Closeable {
    private static final long DEFAULT_INTERVAL_MILLIS = 500L;

    private final MatchingService matchingService;
    private final InGameSessionService inGameSessionService;
    private final ScheduledExecutorService scheduler;
    private final long intervalMillis;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile NioReactorServer server;

    public MatchingManager(MatchingService matchingService, InGameSessionService inGameSessionService) {
        this(matchingService, inGameSessionService, DEFAULT_INTERVAL_MILLIS);
    }

    public MatchingManager(MatchingService matchingService,
                           InGameSessionService inGameSessionService,
                           long intervalMillis) {
        this.matchingService = Objects.requireNonNull(matchingService, "matchingService");
        this.inGameSessionService = Objects.requireNonNull(inGameSessionService, "inGameSessionService");
        this.intervalMillis = intervalMillis;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r);
            t.setName("matching-manager");
            t.setDaemon(true);
            return t;
        });
    }

    public void start(NioReactorServer server) {
        Objects.requireNonNull(server, "server");
        this.server = server;
        if (running.compareAndSet(false, true)) {
            scheduler.scheduleAtFixedRate(this::runMatchLoop, intervalMillis, intervalMillis, TimeUnit.MILLISECONDS);
        }
    }

    private void runMatchLoop() {
        NioReactorServer srv = server;
        if (srv == null) {
            return;
        }
        try {
            MatchingService.Result result = matchingService.tryMatch();
            if (result instanceof MatchingService.Result.Success success) {
                inGameSessionService.createFromGroup(srv, success.group());
            }
        } catch (Exception ignored) {
            // keep loop running even if one iteration fails
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
