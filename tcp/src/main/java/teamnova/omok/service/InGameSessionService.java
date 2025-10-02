package teamnova.omok.service;

import teamnova.omok.handler.register.Type;
import teamnova.omok.nio.ClientSession;
import teamnova.omok.nio.NioReactorServer;
import teamnova.omok.store.InGameSessionStore;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InGameSessionService {
    private final InGameSessionStore store;
    // Active client sessions by userId for broadcasting
    private final ConcurrentMap<String, ClientSession> clients = new ConcurrentHashMap<>();

    public InGameSessionService(InGameSessionStore store) {
        this.store = Objects.requireNonNull(store, "store");
    }

    public void registerClient(String userId, ClientSession session) {
        if (userId != null && session != null) {
            clients.put(userId, session);
        }
    }

    public void unregisterClient(String userId) {
        if (userId != null) clients.remove(userId);
    }

    public Optional<GameSession> findByUser(String userId) {
        return store.findByUserId(userId);
    }

    public void leaveByUser(String userId) {
        store.removeByUserId(userId);
    }

    public GameSession createFromGroup(NioReactorServer server, MatchingService.Group group) {
        List<String> userIds = new ArrayList<>();
        group.getTickets().forEach(t -> userIds.add(t.id));
        GameSession session = new GameSession(userIds);
        store.save(session);
        broadcastJoin(server, session);
        return session;
    }

    private void broadcastJoin(NioReactorServer server, GameSession session) {
        // Build JSON object string with session and users info
        StringBuilder sb = new StringBuilder(256);
        sb.append('{')
          .append("\"sessionId\":\"").append(session.getId()).append('\"')
          .append(',')
          .append("\"createdAt\":" ).append(session.getCreatedAt())
          .append(',')
          .append("\"users\":[");
        List<String> uids = session.getUserIds();
        for (int i = 0; i < uids.size(); i++) {
            String uid = uids.get(i);
            // Placeholder user info; in future, fetch from MySQL 'users' table
            sb.append('{')
              .append("\"userId\":\"").append(escape(uid)).append('\"')
              .append(',')
              .append("\"displayName\":\"").append(escape(uid)).append('\"')
              .append(',')
              .append("\"profileIconCode\":").append(0)
              .append('}');
            if (i < uids.size() - 1) sb.append(',');
        }
        sb.append(']').append('}');
        byte[] payload = sb.toString().getBytes(StandardCharsets.UTF_8);
        for (String uid : session.getUserIds()) {
            ClientSession cs = clients.get(uid);
            if (cs != null) {
                cs.enqueueResponse(Type.JOIN_IN_GAME_SESSION, 0L, payload);
                server.enqueueSelectorTask(cs::enableWriteInterest);
            }
        }
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
