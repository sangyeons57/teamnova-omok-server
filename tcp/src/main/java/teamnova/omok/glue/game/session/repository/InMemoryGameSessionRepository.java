package teamnova.omok.glue.game.session.repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import teamnova.omok.glue.game.session.interfaces.GameSessionRepository;
import teamnova.omok.glue.game.session.model.GameSession;
import teamnova.omok.glue.game.session.model.vo.GameSessionId;

/**
 * 메모리에 게임 세션을 보관하는 단순 저장소 구현.
 */
public final class InMemoryGameSessionRepository implements GameSessionRepository {
    private final Map<GameSessionId, GameSession> sessionsById = new ConcurrentHashMap<>();
    private final Map<String, GameSessionId> sessionIdByUser = new ConcurrentHashMap<>();

    @Override
    public GameSession save(GameSession session) {
        sessionsById.put(session.sessionId(), session);
        for (String uid : session.getUserIds()) {
            sessionIdByUser.put(uid, session.sessionId());
        }
        return session;
    }

    @Override
    public Optional<GameSession> findById(GameSessionId id) {
        return Optional.ofNullable(sessionsById.get(id));
    }

    @Override
    public Optional<GameSession> findByUserId(String userId) {
        GameSessionId sessionId = sessionIdByUser.get(userId);
        return sessionId == null ? Optional.empty() : Optional.ofNullable(sessionsById.get(sessionId));
    }

    @Override
    public Optional<GameSession> removeById(GameSessionId id) {
        GameSession removed = sessionsById.remove(id);
        if (removed != null) {
            for (String uid : removed.getUserIds()) {
                sessionIdByUser.remove(uid, id);
            }
            return Optional.of(removed);
        }
        return Optional.empty();
    }

    @Override
    public Optional<GameSession> removeByUserId(String userId) {
        GameSessionId id = sessionIdByUser.remove(userId);
        if (id == null) {
            return Optional.empty();
        }
        GameSession removed = sessionsById.remove(id);
        if (removed != null) {
            for (String uid : removed.getUserIds()) {
                sessionIdByUser.remove(uid, id);
            }
            return Optional.of(removed);
        }
        return Optional.empty();
    }
}
