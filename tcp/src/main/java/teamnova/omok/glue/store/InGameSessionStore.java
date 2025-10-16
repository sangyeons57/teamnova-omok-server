package teamnova.omok.glue.store;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import teamnova.omok.glue.service.BoardService;
import teamnova.omok.glue.service.ScoreService;
import teamnova.omok.glue.service.TurnService;
import teamnova.omok.glue.state.game.GameStateHub;

/**
 * Central registry for active game sessions and their state managers.
 */
public class InGameSessionStore {
    private final Map<UUID, GameSession> sessionsById = new ConcurrentHashMap<>();
    private final Map<String, UUID> sessionIdByUser = new ConcurrentHashMap<>();
    private final Map<UUID, GameStateHub> managersById = new ConcurrentHashMap<>();

    private final BoardService boardService;
    private final TurnService turnService;
    private final ScoreService scoreService;

    public InGameSessionStore(BoardService boardService,
                              TurnService turnService,
                              ScoreService scoreService) {
        this.boardService = boardService;
        this.turnService = turnService;
        this.scoreService = scoreService;
    }

    public GameSession save(GameSession session) {
        sessionsById.put(session.getId(), session);
        for (String uid : session.getUserIds()) {
            sessionIdByUser.put(uid, session.getId());
        }
        ensureManager(session);
        return session;
    }

    public Optional<GameSession> findById(UUID id) {
        return Optional.ofNullable(sessionsById.get(id));
    }

    public Optional<GameSession> findByUserId(String userId) {
        UUID id = sessionIdByUser.get(userId);
        return id == null ? Optional.empty() : Optional.ofNullable(sessionsById.get(id));
    }

    public Optional<GameStateHub> stateManager(UUID sessionId) {
        return Optional.ofNullable(managersById.get(sessionId));
    }

    public Optional<GameStateHub> stateManagerByUser(String userId) {
        UUID sessionId = sessionIdByUser.get(userId);
        if (sessionId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(managersById.get(sessionId));
    }

    public GameStateHub ensureManager(GameSession session) {
        return managersById.computeIfAbsent(
            session.getId(),
            id -> new GameStateHub(session, boardService, turnService, scoreService)
        );
    }

    public void removeById(UUID id) {
        GameSession removed = sessionsById.remove(id);
        if (removed != null) {
            managersById.remove(id);
            for (String uid : removed.getUserIds()) {
                sessionIdByUser.remove(uid, id);
            }
        } else {
            managersById.remove(id);
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
            managersById.remove(id);
        }
    }

    public void updateSessions(long now) {
        for (GameStateHub manager : managersById.values()) {
            manager.process(now);
        }
    }
}
