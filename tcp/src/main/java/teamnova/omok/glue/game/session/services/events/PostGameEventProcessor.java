package teamnova.omok.glue.game.session.services.events;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import teamnova.omok.glue.game.session.interfaces.manager.TurnTimeoutScheduler;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionAccess;
import teamnova.omok.glue.game.session.model.GameSession;
import teamnova.omok.glue.game.session.model.messages.BoardSnapshotUpdate;
import teamnova.omok.glue.game.session.model.messages.GameCompletionNotice;
import teamnova.omok.glue.game.session.model.messages.PostGameDecisionPrompt;
import teamnova.omok.glue.game.session.model.messages.PostGameDecisionUpdate;
import teamnova.omok.glue.game.session.model.messages.PostGameResolution;
import teamnova.omok.glue.game.session.model.result.PostGameDecisionResult;
import teamnova.omok.glue.game.session.model.result.PostGameDecisionStatus;
import teamnova.omok.glue.game.session.model.vo.GameSessionId;
import teamnova.omok.glue.game.session.services.GameSessionDependencies;
import teamnova.omok.glue.game.session.states.GameStateHub;
import teamnova.omok.glue.game.session.states.event.DecisionTimeoutEvent;
import teamnova.omok.glue.game.session.states.event.PostGameDecisionEvent;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContext;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContextService;

/**
 * Coordinates post-game decision flows and aggregates resulting side effects.
 */
public final class PostGameEventProcessor {

    private final GameSessionDependencies deps;

    public PostGameEventProcessor(GameSessionDependencies deps) {
        this.deps = Objects.requireNonNull(deps, "deps");
    }

    public void handleDecisionCompletion(GameStateHub manager,
                                         GameSessionStateContext ctx,
                                         PostGameDecisionEvent event,
                                         TurnTimeoutScheduler.TurnTimeoutConsumer timeoutConsumer) {
        Objects.requireNonNull(manager, "manager");
        Objects.requireNonNull(ctx, "ctx");
        Objects.requireNonNull(event, "event");
        Objects.requireNonNull(timeoutConsumer, "timeoutConsumer");
        GameSessionStateContextService contextService = deps.contextService();
        PostGameDecisionResult result = contextService.postGame().consumeDecisionResult(ctx);
        if (result == null) {
            result = PostGameDecisionResult.rejected(manager.session(),
                event.userId(),
                PostGameDecisionStatus.SESSION_CLOSED);
        }
        deps.messenger().respondPostGameDecision(event.userId(), event.requestId(), result);
        drainSideEffects(ctx, timeoutConsumer);
    }

    public void drainSideEffects(GameSessionStateContext ctx,
                                 TurnTimeoutScheduler.TurnTimeoutConsumer timeoutConsumer) {
        Objects.requireNonNull(ctx, "ctx");
        Objects.requireNonNull(timeoutConsumer, "timeoutConsumer");
        GameSessionStateContextService contextService = deps.contextService();
        BoardSnapshotUpdate boardUpdate = contextService.postGame().consumeBoardSnapshot(ctx);
        if (boardUpdate != null) {
            deps.messenger().broadcastBoardSnapshot(boardUpdate);
        }
        GameCompletionNotice completion = contextService.postGame().consumeGameCompletion(ctx);
        if (completion != null) {
            deps.messenger().broadcastGameCompleted(completion.session());
        }
        PostGameDecisionPrompt prompt = contextService.postGame().consumeDecisionPrompt(ctx);
        if (prompt != null) {
            deps.messenger().broadcastPostGamePrompt(prompt);
            scheduleDecisionTimeout(prompt.session(), prompt.deadlineAt(), timeoutConsumer);
        }
        PostGameDecisionUpdate update = contextService.postGame().consumeDecisionUpdate(ctx);
        if (update != null) {
            deps.messenger().broadcastPostGameDecisionUpdate(update);
        }
        PostGameResolution resolution = contextService.postGame().consumePostGameResolution(ctx);
        if (resolution != null) {
            handlePostGameResolution(resolution);
        }
    }

    private void scheduleDecisionTimeout(GameSessionAccess session,
                                         long deadlineAt,
                                         TurnTimeoutScheduler.TurnTimeoutConsumer timeoutConsumer) {
        Objects.requireNonNull(session, "session");
        deps.decisionTimeoutScheduler().schedule(session.sessionId(), deadlineAt,
            () -> handleDecisionTimeout(timeoutConsumer, session.sessionId()));
    }

    private void handleDecisionTimeout(TurnTimeoutScheduler.TurnTimeoutConsumer timeoutConsumer,
                                       GameSessionId sessionId) {
        Objects.requireNonNull(timeoutConsumer, "timeoutConsumer");
        Optional<GameSession> optionalSession = deps.repository().findById(sessionId);
        if (optionalSession.isEmpty()) {
            return;
        }
        GameSession session = optionalSession.get();
        GameStateHub manager = deps.runtime().ensure(session);
        long now = System.currentTimeMillis();
        DecisionTimeoutEvent event = new DecisionTimeoutEvent(now);
        manager.submit(event, ctx -> drainSideEffects(ctx, timeoutConsumer));
    }

    private void handlePostGameResolution(PostGameResolution resolution) {
        GameSessionAccess session = resolution.session();
        cancelAllTimers(session.sessionId());
        if (resolution.type() == PostGameResolution.ResolutionType.REMATCH) {
            handleRematchResolution(resolution);
        } else {
            handleSessionTermination(session, resolution.disconnected());
        }
    }

    private void cancelAllTimers(GameSessionId sessionId) {
        deps.turnTimeoutScheduler().cancel(sessionId);
        deps.decisionTimeoutScheduler().cancel(sessionId);
    }

    private void handleRematchResolution(PostGameResolution resolution) {
        GameSessionAccess oldSession = resolution.session();
        List<String> participants = resolution.rematchParticipants();
        if (participants.size() < 2) {
            handleSessionTermination(oldSession, resolution.disconnected());
            return;
        }
        GameSession newSession = new GameSession(participants);
        newSession.setRulesContext(deps.ruleManager().prepareRules(newSession));
        deps.repository().save(newSession);
        deps.runtime().ensure(newSession);
        deps.messenger().broadcastRematchStarted(oldSession, newSession, participants);
        deps.messenger().broadcastJoin(newSession);
        deps.messenger().broadcastSessionTerminated(oldSession, resolution.disconnected());
        deps.repository().removeById(oldSession.sessionId()).ifPresent(deps.runtime()::remove);
    }

    private void handleSessionTermination(GameSessionAccess session, List<String> disconnected) {
        deps.messenger().broadcastSessionTerminated(session, disconnected);
        deps.repository().removeById(session.sessionId()).ifPresent(deps.runtime()::remove);
    }
}
