package teamnova.omok.glue.client.session;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import teamnova.omok.core.nio.NioClientConnection;
import teamnova.omok.core.nio.NioReactorServer;
import teamnova.omok.glue.client.session.interfaces.ClientSessionHandle;
import teamnova.omok.glue.client.session.interfaces.ClientSessionLifecycleListener;
import teamnova.omok.glue.client.session.model.ClientSession;
import teamnova.omok.glue.client.session.services.ClientSessionDirectory;
import teamnova.omok.glue.client.session.services.ClientSessionLifecycleService;
import teamnova.omok.glue.client.session.services.ClientSessionMessagePublisher;
import teamnova.omok.glue.game.session.model.PlayerResult;
import teamnova.omok.glue.game.session.services.BoardService;
import teamnova.omok.glue.game.session.services.GameSessionMessagePublisher;
import teamnova.omok.glue.handler.register.Type;

/**
 * Singleton facade that exposes session operations while delegating implementation
 * details to dedicated services.
 */
public final class ClientSessionManager implements ClientSessionLifecycleListener {
    private static ClientSessionManager INSTANCE;
    public static ClientSessionManager Init() {
        INSTANCE = new ClientSessionManager();
        return INSTANCE;
    }

    public static ClientSessionManager getInstance() {
        if (INSTANCE == null) {
            throw new IllegalStateException("ClientSessionManager not initialized");
        }
        return INSTANCE;
    }

    private final ClientSessionDirectory directory = new ClientSessionDirectory();
    private final ClientSessionMessagePublisher clientPublisher = new ClientSessionMessagePublisher();
    private final GameSessionMessagePublisher gamePublisher = new GameSessionMessagePublisher(
        directory,
        new BoardService()
    );
    private final ClientSessionLifecycleService lifecycleService = new ClientSessionLifecycleService(directory, clientPublisher);

    private ClientSessionManager() {
    }

    public ClientSessionHandle registerConnection(NioClientConnection connection, NioReactorServer server) {
        Objects.requireNonNull(connection, "connection");
        Objects.requireNonNull(server, "server");
        return directory.register(connection, server, this);
    }

    public GameSessionMessagePublisher gamePublisher() {
        return gamePublisher;
    }

    public ClientSessionMessagePublisher.SessionChannel clientPublisher(ClientSessionHandle handle) {
        return clientPublisher.session(handle);
    }

    public Optional<ClientSessionMessagePublisher.SessionChannel> clientPublisher(String userId) {
        return directory.findByUser(userId).map(clientPublisher::session);
    }

    public void onAuthenticated(ClientSessionHandle handle, String userId, String role, String scope) {
        lifecycleService.onAuthenticated(handle, userId, role, scope);
    }

    public void onAuthenticationCleared(ClientSessionHandle handle) {
        lifecycleService.onAuthenticationCleared(handle);
    }

    @Override
    public void onSessionClosed(ClientSessionHandle session) {
        lifecycleService.onSessionClosed(session);
    }

    public Optional<ClientSessionHandle> findSession(String userId) {
        return directory.findByUser(userId);
    }

    public void forEachAuthenticated(Consumer<ClientSessionHandle> consumer) {
        directory.forEachAuthenticated(consumer);
    }

    public void send(String userId, Type type, long requestId, byte[] payload) {
        directory.send(userId, type, requestId, payload);
    }

    public void broadcast(Collection<String> userIds, Type type, byte[] payload) {
        directory.broadcast(userIds, type, payload);
    }

    public ClientSession.ClientSessionMetrics recordOutcome(String userId, PlayerResult result) {
        return directory.recordOutcome(userId, result);
    }
}
