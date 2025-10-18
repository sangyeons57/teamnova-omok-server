package teamnova.omok.glue.game.session.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import teamnova.omok.core.nio.NioReactorServer;
import teamnova.omok.glue.game.session.model.PostGameDecision;
import teamnova.omok.glue.game.session.interfaces.GameSessionMessenger;
import teamnova.omok.glue.game.session.interfaces.GameSessionOperations;
import teamnova.omok.glue.game.session.interfaces.GameSessionEventProcessor;
import teamnova.omok.glue.game.session.interfaces.GameSessionRepository;
import teamnova.omok.glue.game.session.interfaces.GameSessionRuntime;
import teamnova.omok.glue.game.session.interfaces.GameTurnService;
import teamnova.omok.glue.game.session.model.GameSession;
import teamnova.omok.glue.rule.RuleManager;
import teamnova.omok.modules.matching.models.MatchGroup;

public class InGameSessionService implements GameSessionOperations {
    private final GameSessionRepository repository;
    private final GameSessionRuntime runtime;
    private final GameTurnService turnService;
    private final GameSessionMessenger messagePublisher;
    private final GameSessionEventProcessor eventService;
    private final RuleManager ruleManager;

    public InGameSessionService(GameSessionRepository repository,
                                GameSessionRuntime runtime,
                                GameTurnService turnService,
                                GameSessionMessenger messagePublisher,
                                GameSessionEventProcessor eventService,
                                RuleManager ruleManager) {
        this.repository = Objects.requireNonNull(repository, "repository");
        this.runtime = Objects.requireNonNull(runtime, "runtime");
        this.turnService = Objects.requireNonNull(turnService, "turnService");
        this.messagePublisher = Objects.requireNonNull(messagePublisher, "messagePublisher");
        this.eventService = Objects.requireNonNull(eventService, "eventService");
        this.ruleManager = Objects.requireNonNull(ruleManager, "ruleManager");
    }

    @Override
    public void attachServer(NioReactorServer server) {
        Objects.requireNonNull(server, "server");
        // Per-connection server references are passed during ClientSession creation.
    }

    @Override
    public Optional<GameSession> findByUser(String userId) {
        return repository.findByUserId(userId);
    }

    @Override
    public void leaveByUser(String userId) {
        repository.findByUserId(userId).ifPresent(session -> {
            eventService.cancelAllTimers(session.sessionId());
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
        repository.removeByUserId(userId).ifPresent(runtime::remove);
    }

    @Override
    public void handleClientDisconnected(String userId) {
        Objects.requireNonNull(userId, "userId");
        repository.findByUserId(userId).ifPresent(session -> {
            boolean shouldSkip = false;
            int expectedTurn = -1;
            boolean newlyDisconnected = false;
            session.lock().lock();
            try {
                newlyDisconnected = session.markDisconnected(userId);
                if (session.isGameStarted() && !session.isGameFinished()) {
                    GameTurnService.TurnSnapshot snapshot = turnService.snapshot(session.getTurnStore(), session.getUserIds());
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

    @Override
    public boolean submitReady(String userId, long requestId) {
        return eventService.submitReady(userId, requestId);
    }

    @Override
    public boolean submitMove(String userId, long requestId, int x, int y) {
        return eventService.submitMove(userId, requestId, x, y);
    }

    @Override
    public boolean submitPostGameDecision(String userId, long requestId, PostGameDecision decision) {
        return eventService.submitPostGameDecision(userId, requestId, decision);
    }

    @Override
    public void createFromGroup(NioReactorServer server, MatchGroup group) {
        attachServer(server);
        List<String> userIds = new ArrayList<>();
        group.tickets().forEach(t -> userIds.add(t.id()));
        GameSession session = new GameSession(userIds);
        Map<String, Integer> knownScores = new HashMap<>();
        group.tickets().forEach(ticket -> knownScores.put(ticket.id(), ticket.rating()));
        session.setRulesContext(ruleManager.prepareRules(session, knownScores));
        repository.save(session);
        runtime.ensure(session);
        messagePublisher.broadcastJoin(session);
    }
}
