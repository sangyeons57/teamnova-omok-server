package teamnova.omok.store;

import teamnova.omok.application.register.GameSessionStateRegister;
import teamnova.omok.domain.session.game.GameSession;
import teamnova.omok.domain.session.game.entity.state.GameSessionStateHolder;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GameSessionStore {

    private final Map<UUID, GameSession> sessionsById;
    private final Map<String, UUID> sessionIdByUser;
    public GameSessionStore() {
        this.sessionsById = new ConcurrentHashMap<>();
        this.sessionIdByUser = new ConcurrentHashMap<>();
    }


    public void save(GameSession session) {
        sessionsById.put(session.getId(), session);
        for (String uid : session.getUserIds()) {
            sessionIdByUser.put(uid, session.getId());
        }
    }

    public Optional<GameSession> findById(UUID id) {
        return Optional.ofNullable(sessionsById.get(id));
    }

    public Optional<GameSession> findByUserId(String userId) {
        UUID id = sessionIdByUser.get(userId);
        return id == null ? Optional.empty() : Optional.ofNullable(sessionsById.get(id));
    }

    public List<GameSession> findAll() {
        return new ArrayList<>(sessionsById.values());
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
            GameSession session = sessionsById.remove(id);
            if (session != null) {
                for (String uid : session.getUserIds()) {
                    sessionIdByUser.remove(uid, id);
                }
            }
        }
    }
}
