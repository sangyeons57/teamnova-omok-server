package teamnova.omok.store;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class InGameSessionStore {
    private final Map<UUID, GameSession> sessionsById = new ConcurrentHashMap<>();
    private final Map<String, UUID> sessionIdByUser = new ConcurrentHashMap<>();

    public GameSession save(GameSession session) {
        sessionsById.put(session.getId(), session);
        for (String uid : session.getUserIds()) {
            sessionIdByUser.put(uid, session.getId());
        }
        return session;
    }

    public Optional<GameSession> findById(UUID id) {
        return Optional.ofNullable(sessionsById.get(id));
    }

    public Optional<GameSession> findByUserId(String userId) {
        UUID id = sessionIdByUser.get(userId);
        return id == null ? Optional.empty() : Optional.ofNullable(sessionsById.get(id));
    }

    public void removeById(UUID id) {
        GameSession removed = sessionsById.remove(id);
        if (removed != null) {
            for (String uid : removed.getUserIds()) {
                sessionIdByUser.remove(uid, id);
            }
        }
    }

    public void removeByUserId(String userId) {
        UUID id = sessionIdByUser.remove(userId);
        if (id != null) {
            GameSession session = sessionsById.get(id);
            if (session != null) {
                for (String uid : session.getUserIds()) {
                    sessionIdByUser.remove(uid, id);
                }
            }
            sessionsById.remove(id);
        }
    }

    public Collection<GameSession> allSessions() {
        return Collections.unmodifiableCollection(sessionsById.values());
    }
}
