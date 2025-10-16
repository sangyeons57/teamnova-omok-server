package teamnova.omok.glue.manager;

import java.io.Closeable;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import teamnova.omok.glue.service.ServiceManager;

/**
 * Top-level coordinator that wires managers together and drives the process lifecycle.
 */
public final class ServerLifecycleManager implements Closeable {
    private final ServiceManager services;
    private final NioManager nioManager;
    private final GameSessionManager gameSessionManager;
    private final MatchingManager matchingManager;
    private final UserSessionManager userSessionManager;
    private final AtomicBoolean started = new AtomicBoolean(false);

    public ServerLifecycleManager(ServiceManager services,
                                  NioManager nioManager,
                                  GameSessionManager gameSessionManager,
                                  MatchingManager matchingManager,
                                  UserSessionManager userSessionManager) {
        this.services = Objects.requireNonNull(services, "services");
        this.nioManager = Objects.requireNonNull(nioManager, "nioManager");
        this.gameSessionManager = Objects.requireNonNull(gameSessionManager, "gameSessionManager");
        this.matchingManager = Objects.requireNonNull(matchingManager, "matchingManager");
        this.userSessionManager = Objects.requireNonNull(userSessionManager, "userSessionManager");
    }

    public void start() {
        if (started.compareAndSet(false, true)) {
            services.start(nioManager.getServer());
            gameSessionManager.start();
            matchingManager.start(nioManager.getServer());
            userSessionManager.start();
        }
    }

    public void runBlocking() {
        if (!started.get()) {
            start();
        }
        nioManager.run();
    }

    @Override
    public void close() {
        try {
            matchingManager.stop();
        } finally {
            try {
                gameSessionManager.stop();
            } finally {
                try {
                    userSessionManager.stop();
                } finally {
                    try {
                        nioManager.close();
                    } finally {
                        services.stop();
                    }
                }
            }
        }
    }
}
