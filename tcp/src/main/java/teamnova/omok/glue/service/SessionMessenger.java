package teamnova.omok.glue.service;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import teamnova.omok.glue.handler.register.Type;
import teamnova.omok.core.nio.ClientSession;
import teamnova.omok.core.nio.NioReactorServer;
import teamnova.omok.glue.store.GameSession;

/**
 * Handles network interactions with connected clients for a game session.
 */
final class SessionMessenger {
    private final ConcurrentMap<String, ClientSession> clients = new ConcurrentHashMap<>();
    private volatile NioReactorServer server;

    void attachServer(NioReactorServer server) {
        this.server = Objects.requireNonNull(server, "server");
    }

    void registerClient(String userId, ClientSession session) {
        if (userId != null && session != null) {
            clients.put(userId, session);
        }
    }

    void unregisterClient(String userId) {
        if (userId != null) {
            clients.remove(userId);
        }
    }

    ClientSession getClient(String userId) {
        return clients.get(userId);
    }

    void broadcast(GameSession session, Type type, byte[] payload) {
        NioReactorServer srv = server;
        if (srv == null || session == null) {
            return;
        }
        for (String uid : session.getUserIds()) {
            ClientSession cs = clients.get(uid);
            if (cs != null) {
                cs.enqueueResponse(type, 0L, payload);
                srv.enqueueSelectorTask(cs::enableWriteInterest);
            }
        }
    }

    void respond(String userId, Type type, long requestId, byte[] payload) {
        NioReactorServer srv = server;
        if (srv == null || userId == null) {
            return;
        }
        ClientSession cs = clients.get(userId);
        if (cs == null) {
            return;
        }
        cs.enqueueResponse(type, requestId, payload);
        srv.enqueueSelectorTask(cs::enableWriteInterest);
    }

}
