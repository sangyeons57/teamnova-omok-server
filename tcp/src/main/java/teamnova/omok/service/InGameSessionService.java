package teamnova.omok.service;

import teamnova.omok.game.PlayerResult;
import teamnova.omok.game.PostGameDecisionType;
import teamnova.omok.nio.ClientSession;
import teamnova.omok.nio.NioReactorServer;
import teamnova.omok.application.SessionMessagePublisher;
import teamnova.omok.application.SessionMessenger;
import teamnova.omok.service.cordinator.DecisionTimeoutCoordinator;
import teamnova.omok.service.cordinator.TurnTimeoutCoordinator;
import teamnova.omok.domain.session.game.GameSession;
import teamnova.omok.domain.session.game.entity.turn.TurnSnapshot;
import teamnova.omok.application.GameSessionManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Map;

public class InGameSessionService {
    private final GameSessionManager store;
    private final SessionMessenger messenger = new SessionMessenger();
    private final SessionMessagePublisher messagePublisher = new SessionMessagePublisher(messenger);
    private final SessionEventService eventService;
    private final RuleService ruleService;

    public InGameSessionService(GameSessionManager store,
                                ScoreService scoreService,
                                RuleService ruleService) {
        Objects.requireNonNull(scoreService, "scoreService");
        this.store = Objects.requireNonNull(store, "store");
        this.ruleService = Objects.requireNonNull(ruleService, "ruleService");
        TurnTimeoutCoordinator timeoutCoordinator = new TurnTimeoutCoordinator();
        DecisionTimeoutCoordinator decisionTimeoutCoordinator = new DecisionTimeoutCoordinator();
        this.eventService = new SessionEventService(
            this.store,
            this.messagePublisher,
            timeoutCoordinator,
            decisionTimeoutCoordinator,
            scoreService,
            ruleService
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
            boolean newlyDisconnected = false;
            session.lock().lock();
            try {
                newlyDisconnected = session.markDisconnected(userId);
                session.updateOutcome(userId, PlayerResult.LOSS);
            } finally {
                session.lock().unlock();
            }
            if (newlyDisconnected) {
                messagePublisher.broadcastPlayerDisconnected(session, userId, "LEFT");
            }
        });
        store.removeByUserId(userId);
    }

    public void handleClientDisconnected(String userId) {
        Objects.requireNonNull(userId, "userId");
        store.findByUserId(userId).ifPresent(session -> {
            boolean shouldSkip = false;
            int expectedTurn = -1;
            boolean newlyDisconnected = false;
            session.lock().lock();
            try {
                newlyDisconnected = session.markDisconnected(userId);
                session.updateOutcome(userId, PlayerResult.LOSS);
                if (session.isGameStarted() && !session.isGameFinished()) {
                    TurnSnapshot snapshot = session.snapshotTurn();
                    if (snapshot != null && userId.equals(snapshot.currentPlayerId())) {
                        shouldSkip = true;
                        expectedTurn = snapshot.turnNumber();
                    }
                }
            } finally {
                session.lock().unlock();
            }
            if (newlyDisconnected) {
                messagePublisher.broadcastPlayerDisconnected(session, userId, "DISCONNECTED");
            }
            if (shouldSkip && expectedTurn > 0) {
                eventService.skipTurnForDisconnected(session, userId, expectedTurn);
            }
        });
    }

    public boolean submitReady(String userId, long requestId) {
        return eventService.submitReady(userId, requestId);
    }

    public boolean submitMove(String userId, long requestId, int x, int y) {
        return eventService.submitMove(userId, requestId, x, y);
    }

    public boolean submitPostGameDecision(String userId, long requestId, PostGameDecisionType decision) {
        return eventService.submitPostGameDecision(userId, requestId, decision);
    }

    public void createFromGroup(NioReactorServer server, MatchingService.Group group) {
        attachServer(server);
        List<String> userIds = new ArrayList<>();
        group.getTickets().forEach(t -> userIds.add(t.id));
        GameSession session = new GameSession(userIds);
        Map<String, Integer> knownScores = new HashMap<>();
        group.getTickets().forEach(ticket -> knownScores.put(ticket.id, ticket.rating));
        session.setRulesContext(ruleService.prepareRules(session, knownScores, RuleService.DEFAULT_RULE_SELECTION_COUNT));
        store.save(session);
        messagePublisher.broadcastJoin(session);
    }
}
