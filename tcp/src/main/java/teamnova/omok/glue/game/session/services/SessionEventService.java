package teamnova.omok.glue.game.session.services;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import teamnova.omok.glue.game.session.log.GameSessionLogger;
import teamnova.omok.glue.game.session.model.GameSession;
import teamnova.omok.glue.game.session.model.PostGameDecision;
import teamnova.omok.glue.game.session.model.dto.SessionSubmission;
import teamnova.omok.glue.game.session.model.vo.GameSessionId;
import teamnova.omok.glue.game.session.states.GameStateHub;
import teamnova.omok.glue.game.session.states.event.MoveEvent;
import teamnova.omok.glue.game.session.states.event.PostGameDecisionEvent;
import teamnova.omok.glue.game.session.states.event.ReadyEvent;
import teamnova.omok.glue.game.session.states.event.TimeoutEvent;

/**
 * Coordinates submission of session state-machine events. No callbacks.
 */
public final class SessionEventService {

    private final GameSessionDependencies deps;

    public SessionEventService(GameSessionDependencies deps) {
        this.deps = Objects.requireNonNull(deps, "deps");
    }

    public boolean submitReady(String userId,
                               long requestId) {
        Objects.requireNonNull(userId, "userId");
        return withSession(userId, submission -> {
            ReadyEvent event = new ReadyEvent(userId, submission.timestamp(), requestId);
            GameSessionLogger.inbound(submission.session(), "READY", userId, requestId);
            submission.manager().submit(event);
        });
    }

    public boolean submitMove(String userId,
                              long requestId,
                              int x,
                              int y) {
        Objects.requireNonNull(userId, "userId");
        return withSession(userId, submission -> {
            MoveEvent event = new MoveEvent(userId, x, y, submission.timestamp(), requestId);
            GameSessionLogger.inbound(submission.session(), "MOVE", userId, requestId,
                String.format("x=%d y=%d", x, y));
            submission.manager().submit(event);
        });
    }

    public boolean submitPostGameDecision(String userId,
                                          long requestId,
                                          PostGameDecision decision) {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(decision, "decision");
        return withSession(userId, submission -> {
            PostGameDecisionEvent event =
                new PostGameDecisionEvent(userId, decision, submission.timestamp(), requestId);
            GameSessionLogger.inbound(submission.session(), "POST_GAME_DECISION", userId, requestId,
                "decision=" + decision);
            submission.manager().submit(event);
        });
    }

    public void cancelAllTimers(GameSessionId sessionId) {
        // Directly cancel both schedulers
        deps.turnTimeoutScheduler().cancel(sessionId);
        deps.decisionTimeoutScheduler().cancel(sessionId);
    }

    public void skipTurnForDisconnected(GameSession session,
                                        String userId,
                                        int expectedTurnNumber) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(userId, "userId");
        boolean shouldSubmit = false;
        session.lock().lock();
        try {
            if (session.isGameStarted() && !session.isGameFinished()) {
                var current = deps.turnService().snapshot(session);
                shouldSubmit = current != null
                    && current.turnNumber() == expectedTurnNumber
                    && userId.equals(current.currentPlayerId());
            }
        } finally {
            session.lock().unlock();
        }
        if (!shouldSubmit) {
            return;
        }
        deps.turnTimeoutScheduler().cancel(session.sessionId());
        GameStateHub manager = deps.runtime().ensure(session);
        long now = System.currentTimeMillis();
        TimeoutEvent event = new TimeoutEvent(expectedTurnNumber, now);
        manager.submit(event);
    }

    public void handleScheduledTimeout(GameSessionId sessionId,
                                       int expectedTurnNumber) {
        Objects.requireNonNull(sessionId, "sessionId");
        if (!deps.turnTimeoutScheduler().validate(sessionId, expectedTurnNumber)) {
            return;
        }
        Optional<GameSession> optionalSession = deps.repository().findById(sessionId);
        if (optionalSession.isEmpty()) {
            return;
        }
        GameSession session = optionalSession.get();
        GameStateHub manager = deps.runtime().ensure(session);
        deps.turnTimeoutScheduler().clearIfMatches(sessionId, expectedTurnNumber);
        long now = System.currentTimeMillis();
        TimeoutEvent event = new TimeoutEvent(expectedTurnNumber, now);
        manager.submit(event);
    }

    private boolean withSession(String userId, Consumer<SessionSubmission> consumer) {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(consumer, "consumer");
        Optional<GameSession> optionalSession = deps.repository().findByUserId(userId);
        if (optionalSession.isEmpty()) {
            return false;
        }
        GameSession session = optionalSession.get();
        GameStateHub manager = deps.runtime().ensure(session);
        long now = System.currentTimeMillis();
        consumer.accept(new SessionSubmission(session, manager, now));
        return true;
    }
}
