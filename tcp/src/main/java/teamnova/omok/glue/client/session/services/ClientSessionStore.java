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
import teamnova.omok.glue.client.state.manage.ClientStateType;
import teamnova.omok.glue.client.state.model.ClientStateTypeTransition;

/**
 * Pure client-session storage that only tracks registration and user bindings.
 * Higher level services should build on top of this store for messaging or cleanup logic.
 */
public final class ClientSessionStore {

    private static final ClientSessionStore INSTANCE = new ClientSessionStore();
    private ClientSessionStore() { }

    public static ClientSessionStore getInstance() {
        return INSTANCE;
    }

    private final ConcurrentMap<String, ClientSessionHandle> byUser = new ConcurrentHashMap<>();

    public ClientSessionHandle bindUser(ClientSessionHandle session, String userId) {
        Objects.requireNonNull(userId, "userId");

        session.addStateListener(
            new ClientStateTypeTransition(ClientStateType.DISCONNECTED, ClientStateType.TERMINATED),
            sessionHandle -> {
                byUser.remove(userId, sessionHandle);
                sessionHandle.model().clearAuthentication();
            }
        );

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
}
