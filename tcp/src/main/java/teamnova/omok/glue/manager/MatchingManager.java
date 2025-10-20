package teamnova.omok.glue.manager;

import java.io.Closeable;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import teamnova.omok.core.nio.NioReactorServer;
import teamnova.omok.glue.game.session.GameSessionManager;
import teamnova.omok.modules.matching.MatchingGateway;
import teamnova.omok.modules.matching.models.MatchGroup;
import teamnova.omok.modules.matching.models.MatchResult;
import teamnova.omok.modules.matching.models.MatchTicket;

/**
 * Periodically attempts to match waiting players and spin up game sessions.
 */
public final class MatchingManager implements Closeable {
    private static final long DEFAULT_INTERVAL_MILLIS = 100L;

    private static MatchingManager INSTANCE;

    public static MatchingManager Init(GameSessionManager gameSessionManager) {
        INSTANCE = new MatchingManager(
                MatchingGateway.open(),
                gameSessionManager,
                DEFAULT_INTERVAL_MILLIS
        );
        return INSTANCE;
    }

    public static MatchingManager getInstance() {
        if( INSTANCE == null) {
            throw new IllegalStateException("MatchingManager not initialized");
        }
        return INSTANCE;
    }

    private final MatchingGateway.Handle matchingGateway;
    private final GameSessionManager gameSessionManager;
    private final ScheduledExecutorService scheduler;
    private final long intervalMillis;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private volatile NioReactorServer server;

    private MatchingManager(MatchingGateway.Handle matchingGateway,
                           GameSessionManager gameSessionManager,
                           long intervalMillis) {
        this.matchingGateway = Objects.requireNonNull(matchingGateway, "matchingGateway");
        this.gameSessionManager = Objects.requireNonNull(gameSessionManager, "gameSessionManager");
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
            MatchResult result = matchingGateway.tryMatchOnce();
            if (result instanceof MatchResult.Success(MatchGroup group)) {
                gameSessionManager.createFromGroup(srv, group);
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

    public void enqueue(MatchTicket matchTicket) {
        matchingGateway.enqueue(matchTicket);
    }

    public void enqueue(String userId, int rating, Set<Integer> matchSet){
        matchingGateway.enqueue(MatchTicket.create(userId, rating, matchSet));
    }

    public void cancel(String ticketId) {
        matchingGateway.cancel(ticketId);
    }

    @Override
    public void close() {
        stop();
    }
}
