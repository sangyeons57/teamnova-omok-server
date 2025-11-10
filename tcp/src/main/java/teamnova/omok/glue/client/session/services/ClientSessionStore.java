package teamnova.omok.glue.client.session.services;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

import teamnova.omok.core.nio.NioClientConnection;
import teamnova.omok.core.nio.NioReactorServer;
import teamnova.omok.glue.client.session.interfaces.ClientSessionHandle;

/**
 * Pure client-session storage that only tracks registration and user bindings.
 * Higher level services should build on top of this store for messaging or cleanup logic.
 */
public final class ClientSessionStore {

    private final Set<ClientSessionHandle> sessions = ConcurrentHashMap.newKeySet();
    private final ConcurrentMap<String, ClientSessionHandle> byUser = new ConcurrentHashMap<>();

    public ClientSessionHandle register(NioClientConnection connection,
                                        NioReactorServer server) {
        Objects.requireNonNull(connection, "connection");
        Objects.requireNonNull(server, "server");
        ClientSessionHandle session = new ManagedClientSession(connection, server, this);
        sessions.add(session);
        return session;
    }

    public void add(ClientSessionHandle session) {
        if (session != null) {
            sessions.add(session);
        }
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

    public Set<ClientSessionHandle> sessions() {
        return Set.copyOf(sessions);
    }
}
