package teamnova.omok.glue.client.session.services;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

import teamnova.omok.core.nio.NioClientConnection;
import teamnova.omok.core.nio.NioReactorServer;
import teamnova.omok.glue.client.session.interfaces.ClientSessionHandle;
import teamnova.omok.glue.client.session.interfaces.ClientSessionLifecycleListener;
import teamnova.omok.glue.client.session.model.ClientSession;
import teamnova.omok.glue.game.session.model.PlayerResult;
import teamnova.omok.glue.handler.register.Type;

/**
 * Thread-safe registry that tracks active client sessions and user bindings.
 */
public final class ClientSessionDirectory {

    private final Set<ClientSessionHandle> sessions = ConcurrentHashMap.newKeySet();
    private final ConcurrentMap<String, ClientSessionHandle> byUser = new ConcurrentHashMap<>();

    public ClientSessionHandle register(NioClientConnection connection,
                                        NioReactorServer server,
                                        ClientSessionLifecycleListener listener) {
        Objects.requireNonNull(connection, "connection");
        Objects.requireNonNull(server, "server");
        Objects.requireNonNull(listener, "listener");
        ClientSessionHandle session = new ManagedClientSession(connection, server, listener);
        sessions.add(session);
        return session;
    }

    public void remove(ClientSessionHandle session) {
        if (session != null) {
            sessions.remove(session);
        }
    }

    public ClientSessionHandle bindUser(String userId, ClientSessionHandle session) {
        if (userId == null || session == null) {
            return null;
        }
        return byUser.put(userId, session);
    }

    public boolean unbindUser(String userId, ClientSessionHandle session) {
        if (userId == null || session == null) {
            return false;
        }
        return byUser.remove(userId, session);
    }

    public Optional<ClientSessionHandle> findByUser(String userId) {
        return Optional.ofNullable(userId).map(byUser::get);
    }

    public void forEachAuthenticated(Consumer<ClientSessionHandle> consumer) {
        if (consumer == null) {
            return;
        }
        byUser.forEach((userId, session) -> {
            if (session != null) {
                consumer.accept(session);
            }
        });
    }

    public void send(String userId, Type type, long requestId, byte[] payload) {
        if (userId == null) {
            return;
        }
        ClientSessionHandle session = byUser.get(userId);
        if (session != null) {
            session.enqueueResponse(type, requestId, payload);
        }
    }

    public void broadcast(Collection<String> userIds, Type type, byte[] payload) {
        if (userIds == null || userIds.isEmpty()) {
            return;
        }
        for (String uid : userIds) {
            send(uid, type, 0L, payload);
        }
    }

    public ClientSession.ClientSessionMetrics recordOutcome(String userId, PlayerResult result) {
        ClientSessionHandle session = byUser.get(userId);
        if (session == null) {
            return new ClientSession.ClientSessionMetrics(0, 0, 0, 0);
        }
        return session.registerOutcome(result);
    }
}
