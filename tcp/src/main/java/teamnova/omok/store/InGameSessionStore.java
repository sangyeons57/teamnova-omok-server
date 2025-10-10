package teamnova.omok.store;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import teamnova.omok.service.BoardService;
import teamnova.omok.service.OutcomeService;
import teamnova.omok.service.TurnService;
import teamnova.omok.state.game.manage.GameSessionStateManager;

/**
 * Central registry for active game sessions and their state managers.
 */
public class InGameSessionStore {
    private final Map<UUID, GameSession> sessionsById = new ConcurrentHashMap<>();
    private final Map<String, UUID> sessionIdByUser = new ConcurrentHashMap<>();
    private final Map<UUID, GameSessionStateManager> managersById = new ConcurrentHashMap<>();

    private final BoardService boardService;
    private final TurnService turnService;
    private final OutcomeService outcomeService;

    public InGameSessionStore(BoardService boardService,
                              TurnService turnService,
                              OutcomeService outcomeService) {
        this.boardService = boardService;
        this.turnService = turnService;
        this.outcomeService = outcomeService;
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

    public Optional<GameSessionStateManager> stateManager(UUID sessionId) {
        return Optional.ofNullable(managersById.get(sessionId));
    }

    public Optional<GameSessionStateManager> stateManagerByUser(String userId) {
        UUID sessionId = sessionIdByUser.get(userId);
        if (sessionId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(managersById.get(sessionId));
    }

    public GameSessionStateManager ensureManager(GameSession session) {
        return managersById.computeIfAbsent(
            session.getId(),
            id -> new GameSessionStateManager(session, boardService, turnService, outcomeService)
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

    public Collection<GameSession> allSessions() {
        return Collections.unmodifiableCollection(sessionsById.values());
    }

    public void updateSessions(long now) {
        for (GameSessionStateManager manager : managersById.values()) {
            manager.process(now);
        }
    }
}
