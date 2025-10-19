package teamnova.omok.glue.game.session.services;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import teamnova.omok.glue.game.session.interfaces.manager.TurnTimeoutScheduler;
import teamnova.omok.glue.game.session.model.GameSession;
import teamnova.omok.glue.game.session.model.PostGameDecision;
import teamnova.omok.glue.game.session.model.dto.SessionSubmission;
import teamnova.omok.glue.game.session.model.vo.GameSessionId;
import teamnova.omok.glue.game.session.states.GameStateHub;
import teamnova.omok.glue.game.session.states.event.MoveEvent;
import teamnova.omok.glue.game.session.states.event.PostGameDecisionEvent;
import teamnova.omok.glue.game.session.states.event.ReadyEvent;
import teamnova.omok.glue.game.session.services.events.MoveEventProcessor;
import teamnova.omok.glue.game.session.services.events.PostGameEventProcessor;
import teamnova.omok.glue.game.session.services.events.ReadyEventProcessor;
import teamnova.omok.glue.game.session.services.events.TimeoutEventProcessor;

/**
 * Coordinates submission of session state-machine events and delegates processing.
 */
public final class SessionEventService {

    private final GameSessionDependencies deps;
    private final PostGameEventProcessor postGameProcessor;
    private final TimeoutEventProcessor timeoutProcessor;
    private final ReadyEventProcessor readyProcessor;
    private final MoveEventProcessor moveProcessor;

    public SessionEventService(GameSessionDependencies deps) {
        this.deps = Objects.requireNonNull(deps, "deps");
        this.postGameProcessor = new PostGameEventProcessor(deps);
        this.timeoutProcessor = new TimeoutEventProcessor(deps, postGameProcessor);
        this.readyProcessor = new ReadyEventProcessor(deps, timeoutProcessor, postGameProcessor);
        this.moveProcessor = new MoveEventProcessor(deps, timeoutProcessor, postGameProcessor);
    }

    public boolean submitReady(TurnTimeoutScheduler.TurnTimeoutConsumer timeoutConsumer,
                               String userId,
                               long requestId) {
        Objects.requireNonNull(timeoutConsumer, "timeoutConsumer");
        Objects.requireNonNull(userId, "userId");
        return withSession(userId, submission -> {
            ReadyEvent event = new ReadyEvent(userId, submission.timestamp(), requestId);
            submission.manager().submit(event,
                ctx -> readyProcessor.handleCompletion(timeoutConsumer, submission.manager(), ctx, event));
        });
    }

    public boolean submitMove(TurnTimeoutScheduler.TurnTimeoutConsumer timeoutConsumer,
                              String userId,
                              long requestId,
                              int x,
                              int y) {
        Objects.requireNonNull(timeoutConsumer, "timeoutConsumer");
        Objects.requireNonNull(userId, "userId");
        return withSession(userId, submission -> {
            MoveEvent event = new MoveEvent(userId, x, y, submission.timestamp(), requestId);
            submission.manager().submit(event,
                ctx -> moveProcessor.handleCompletion(timeoutConsumer, submission.manager(), ctx, event));
        });
    }

    public boolean submitPostGameDecision(TurnTimeoutScheduler.TurnTimeoutConsumer timeoutConsumer,
                                          String userId,
                                          long requestId,
                                          PostGameDecision decision) {
        Objects.requireNonNull(timeoutConsumer, "timeoutConsumer");
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(decision, "decision");
        return withSession(userId, submission -> {
            PostGameDecisionEvent event =
                new PostGameDecisionEvent(userId, decision, submission.timestamp(), requestId);
            submission.manager().submit(event,
                ctx -> postGameProcessor.handleDecisionCompletion(submission.manager(), ctx, event, timeoutConsumer));
        });
    }

    public void cancelAllTimers(GameSessionId sessionId) {
        timeoutProcessor.cancelAllTimers(sessionId);
    }

    public void skipTurnForDisconnected(GameSession session,
                                        String userId,
                                        int expectedTurnNumber,
                                        TurnTimeoutScheduler.TurnTimeoutConsumer timeoutConsumer) {
        timeoutProcessor.skipTurnForDisconnected(session, userId, expectedTurnNumber, timeoutConsumer);
    }

    public void handleScheduledTimeout(GameSessionId sessionId,
                                       int expectedTurnNumber,
                                       TurnTimeoutScheduler.TurnTimeoutConsumer timeoutConsumer) {
        timeoutProcessor.handleScheduledTimeout(sessionId, expectedTurnNumber, timeoutConsumer);
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
