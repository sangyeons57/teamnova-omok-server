package teamnova.omok.service;

import teamnova.omok.game.PlayerResult;
import teamnova.omok.game.PostGameDecision;
import teamnova.omok.nio.ClientSession;
import teamnova.omok.nio.NioReactorServer;
import teamnova.omok.store.GameSession;
import teamnova.omok.store.InGameSessionStore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class InGameSessionService {
    private final InGameSessionStore store;
    private final BoardService boardService;
    private final TurnService turnService;
    private final SessionMessenger messenger = new SessionMessenger();
    private final SessionMessagePublisher messagePublisher = new SessionMessagePublisher(messenger);
    private final SessionEventService eventService;
    /*
     * Post-game networking contract (planned):
     *
     * Broadcast: GAME_SESSION_COMPLETED (already live)
     *   {
     *     "sessionId": "<uuid>",
     *     "outcomes": [
     *       { "userId": "<id>", "result": "WIN|LOSS|DRAW" }
     *     ]
     *   }
     *
     * Broadcast: GAME_POST_DECISION_PROMPT
     *   {
     *     "sessionId": "<uuid>",
     *     "deadlineAt": <epochMillis>,
     *     "options": ["REMATCH", "LEAVE"],
     *     "autoAction": "LEAVE"  // what happens if no reply before deadline
     *   }
     *
     * Client request: POST_GAME_DECISION
     *   {
     *     "sessionId": "<uuid>",
     *     "decision": "REMATCH|LEAVE"
     *   }
     *
     * Server response (ack/error) on POST_GAME_DECISION
     *   Success:
     *     { "status": "OK" }
     *   Error:
     *     { "status": "ERROR", "reason": "INVALID_PLAYER|TIMEOUT|ALREADY_DECIDED|SESSION_CLOSED|SESSION_NOT_FOUND|INVALID_PAYLOAD" }
     *
     * Optional broadcast: GAME_POST_DECISION_UPDATE
     *   {
     *     "sessionId": "<uuid>",
     *     "decided": [
     *       { "userId": "<id>", "decision": "REMATCH|LEAVE" }
     *     ],
     *     "remaining": ["<userId>", ...]
     *   }
     *
     * Broadcast: GAME_SESSION_REMATCH_STARTED
     *   {
     *     "sessionId": "<previousUuid>",
     *     "rematchSessionId": "<newUuid>",
     *     "participants": ["<userId>", ...]
     *   }
     *
     * Broadcast: GAME_SESSION_TERMINATED
     *   {
     *     "sessionId": "<uuid>",
     *     "disconnected": ["<userId>", ...]
     *   }
     *
     * Planned state flow:
     *   1. OutcomeEvaluatingState ⇒ POST_GAME_DECISION_WAITING (new). On enter: GAME_SESSION_COMPLETED + GAME_POST_DECISION_PROMPT; start 30s timer.
     *   2. Clients send POST_GAME_DECISION; state records REMATCH/LEAVE and can emit GAME_POST_DECISION_UPDATE.
     *   3. When all decide or timer hits (DecisionTimeoutEvent), transition ⇒ POST_GAME_DECISION_RESOLVING.
     *   4. POST_GAME_DECISION_RESOLVING:
     *        • rematchCount ≥ 2 ⇒ SESSION_REMATCH_PREPARING → spawn new session, send GAME_SESSION_REMATCH_STARTED.
     *        • otherwise ⇒ SESSION_TERMINATING → send GAME_SESSION_TERMINATED and drop session from store.
     */

    public InGameSessionService(InGameSessionStore store,
                                BoardService boardService,
                                TurnService turnService,
                                OutcomeService outcomeService) {
        Objects.requireNonNull(outcomeService, "outcomeService");
        this.store = Objects.requireNonNull(store, "store");
        this.boardService = Objects.requireNonNull(boardService, "boardService");
        this.turnService = Objects.requireNonNull(turnService, "turnService");
        TurnTimeoutCoordinator timeoutCoordinator = new TurnTimeoutCoordinator();
        DecisionTimeoutCoordinator decisionTimeoutCoordinator = new DecisionTimeoutCoordinator();
        this.eventService = new SessionEventService(
            this.store,
            this.turnService,
            this.messagePublisher,
            timeoutCoordinator,
            decisionTimeoutCoordinator
        );
    }

    public void attachServer(NioReactorServer server) {
        messenger.attachServer(Objects.requireNonNull(server, "server"));
    }

    public void registerClient(String userId, ClientSession session) {
        messenger.registerClient(userId, session);
    }

    public void unregisterClient(String userId) {
        messenger.unregisterClient(userId);
    }

    public Optional<GameSession> findByUser(String userId) {
        return store.findByUserId(userId);
    }

    public ClientSession getClient(String userId) {
        return messenger.getClient(userId);
    }

    public void leaveByUser(String userId) {
        store.findByUserId(userId).ifPresent(session -> {
            eventService.cancelAllTimers(session.getId());
            session.lock().lock();
            try {
                session.markDisconnected(userId);
                session.updateOutcome(userId, PlayerResult.LOSS);
            } finally {
                session.lock().unlock();
            }
        });
        store.removeByUserId(userId);
    }

    public boolean submitReady(String userId, long requestId) {
        return eventService.submitReady(userId, requestId);
    }

    public boolean submitMove(String userId, long requestId, int x, int y) {
        return eventService.submitMove(userId, requestId, x, y);
    }

    public boolean submitPostGameDecision(String userId, long requestId, PostGameDecision decision) {
        return eventService.submitPostGameDecision(userId, requestId, decision);
    }

    public GameSession createFromGroup(NioReactorServer server, MatchingService.Group group) {
        attachServer(server);
        List<String> userIds = new ArrayList<>();
        group.getTickets().forEach(t -> userIds.add(t.id));
        GameSession session = new GameSession(userIds);
        store.save(session);
        messagePublisher.broadcastJoin(session);
        return session;
    }


    public byte[] boardSnapshot(GameSession session) {
        return boardService.snapshot(session.getBoardStore());
    }

    public boolean isTurnExpired(GameSession session, long now) {
        return eventService.isTurnExpired(session, now);
    }

    public TurnService.TurnSnapshot turnSnapshot(GameSession session) {
        return eventService.turnSnapshot(session);
    }

    public Map<String, Boolean> readyStatesView(GameSession session) {
        return session.readyStatesView();
    }
}
