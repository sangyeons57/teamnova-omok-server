package teamnova.omok.glue.game.session.services.events;

import java.util.Objects;
import java.util.Optional;

import teamnova.omok.glue.game.session.interfaces.manager.TurnTimeoutScheduler;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionParticipantsAccess;
import teamnova.omok.glue.game.session.model.GameSession;
import teamnova.omok.glue.game.session.model.dto.TurnSnapshot;
import teamnova.omok.glue.game.session.model.runtime.TurnPersonalFrame;
import teamnova.omok.glue.game.session.model.vo.GameSessionId;
import teamnova.omok.glue.game.session.services.GameSessionDependencies;
import teamnova.omok.glue.game.session.states.GameStateHub;
import teamnova.omok.glue.game.session.states.event.TimeoutEvent;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContext;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContextService;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateType;

/**
 * Handles turn timeout events, scheduling, and disconnect fallbacks.
 */
public final class TimeoutEventProcessor {

    private final GameSessionDependencies deps;
    private final PostGameEventProcessor postGameProcessor;

    public TimeoutEventProcessor(GameSessionDependencies deps,
                                 PostGameEventProcessor postGameProcessor) {
        this.deps = Objects.requireNonNull(deps, "deps");
        this.postGameProcessor = Objects.requireNonNull(postGameProcessor, "postGameProcessor");
    }

    public void cancelAllTimers(GameSessionId sessionId) {
        Objects.requireNonNull(sessionId, "sessionId");
        deps.turnTimeoutScheduler().cancel(sessionId);
        deps.decisionTimeoutScheduler().cancel(sessionId);
    }

    public void skipTurnForDisconnected(GameSession session,
                                        String userId,
                                        int expectedTurnNumber,
                                        TurnTimeoutScheduler.TurnTimeoutConsumer timeoutConsumer) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(timeoutConsumer, "timeoutConsumer");
        boolean shouldSubmit = false;
        session.lock().lock();
        try {
            if (session.isGameStarted() && !session.isGameFinished()) {
                TurnSnapshot current = deps.turnService().snapshot(session);
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
        manager.submit(event, ctx -> handleTimeoutCompletion(session, manager, ctx, event, timeoutConsumer));
    }

    public void handleScheduledTimeout(GameSessionId sessionId,
                                       int expectedTurnNumber,
                                       TurnTimeoutScheduler.TurnTimeoutConsumer timeoutConsumer) {
        Objects.requireNonNull(sessionId, "sessionId");
        Objects.requireNonNull(timeoutConsumer, "timeoutConsumer");
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
        manager.submit(event, ctx -> handleTimeoutCompletion(session, manager, ctx, event, timeoutConsumer));
    }

    public void handleTimeoutCompletion(GameSession session,
                                        GameStateHub manager,
                                        GameSessionStateContext ctx,
                                        TimeoutEvent event,
                                        TurnTimeoutScheduler.TurnTimeoutConsumer timeoutConsumer) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(manager, "manager");
        Objects.requireNonNull(ctx, "ctx");
        Objects.requireNonNull(event, "event");
        Objects.requireNonNull(timeoutConsumer, "timeoutConsumer");
        GameSessionStateContextService contextService = deps.contextService();
        TurnPersonalFrame frame = contextService.turn().consumeTimeoutOutcome(ctx);
        if (frame == null || !frame.hasTimeoutOutcome()) {
            if (!session.isGameFinished()) {
                TurnSnapshot snapshot = deps.turnService().snapshot(session);
                if (snapshot != null) {
                    scheduleTurnTimeout(session, snapshot, timeoutConsumer);
                }
            }
            postGameProcessor.drainSideEffects(ctx, timeoutConsumer);
            return;
        }
        boolean waiting = manager.currentType() == GameSessionStateType.TURN_WAITING;
        if (frame.timeoutSnapshot() != null && waiting) {
            scheduleTurnTimeout(session, frame.timeoutSnapshot(), timeoutConsumer);
        }
        if (manager.currentType() == GameSessionStateType.COMPLETED) {
            cancelAllTimers(session.sessionId());
        }
        frame.markTimeoutConsumed();
        frame.clearTimeoutOutcome();
        postGameProcessor.drainSideEffects(ctx, timeoutConsumer);
    }

    public void scheduleTurnTimeout(GameSessionParticipantsAccess session,
                                    TurnSnapshot turnSnapshot,
                                    TurnTimeoutScheduler.TurnTimeoutConsumer timeoutConsumer) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(turnSnapshot, "turnSnapshot");
        Objects.requireNonNull(timeoutConsumer, "timeoutConsumer");
        deps.turnTimeoutScheduler().schedule(session, turnSnapshot, timeoutConsumer);
    }
}
