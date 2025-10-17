package teamnova.omok.glue.service;

import teamnova.omok.glue.game.PlayerResult;
import teamnova.omok.glue.game.PostGameDecision;
import teamnova.omok.core.nio.ClientSession;
import teamnova.omok.core.nio.NioReactorServer;
import teamnova.omok.glue.service.cordinator.DecisionTimeoutCoordinator;
import teamnova.omok.glue.service.cordinator.TurnTimeoutCoordinator;
import teamnova.omok.glue.store.GameSession;
import teamnova.omok.glue.store.InGameSessionStore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Map;

import teamnova.omok.glue.rule.RuleManager;
import teamnova.omok.modules.matching.models.MatchGroup;

public class InGameSessionService {
    private final InGameSessionStore store;
    private final TurnService turnService;
    private final SessionMessenger messenger = new SessionMessenger();
    private final SessionMessagePublisher messagePublisher = new SessionMessagePublisher(messenger);
    private final SessionEventService eventService;
    private final RuleManager ruleManager;

    public InGameSessionService(InGameSessionStore store,
                                TurnService turnService,
                                ScoreService scoreService,
                                RuleManager ruleManager) {
        Objects.requireNonNull(scoreService, "scoreService");
        this.store = Objects.requireNonNull(store, "store");
        this.turnService = Objects.requireNonNull(turnService, "turnService");
        this.ruleManager = Objects.requireNonNull(ruleManager, "ruleManager");
        TurnTimeoutCoordinator timeoutCoordinator = new TurnTimeoutCoordinator();
        DecisionTimeoutCoordinator decisionTimeoutCoordinator = new DecisionTimeoutCoordinator();
        this.eventService = new SessionEventService(
            this.store,
            this.turnService,
            this.messagePublisher,
            timeoutCoordinator,
            decisionTimeoutCoordinator,
            scoreService,
            ruleManager
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
                if (session.isGameStarted() && !session.isGameFinished()) {
                    TurnService.TurnSnapshot snapshot = turnService.snapshot(session.getTurnStore(), session.getUserIds());
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

    public boolean submitPostGameDecision(String userId, long requestId, PostGameDecision decision) {
        return eventService.submitPostGameDecision(userId, requestId, decision);
    }

    public void createFromGroup(NioReactorServer server, MatchGroup group) {
        attachServer(server);
        List<String> userIds = new ArrayList<>();
        group.tickets().forEach(t -> userIds.add(t.id()));
        GameSession session = new GameSession(userIds);
        Map<String, Integer> knownScores = new HashMap<>();
        group.tickets().forEach(ticket -> knownScores.put(ticket.id(), ticket.rating()));
        session.setRulesContext(ruleManager.prepareRules(session, knownScores));
        store.save(session);
        messagePublisher.broadcastJoin(session);
    }
}
