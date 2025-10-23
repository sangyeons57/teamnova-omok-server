package teamnova.omok.glue.game.session.states.signal;

import java.util.Set;

import teamnova.omok.glue.game.session.model.dto.GameSessionServices;
import teamnova.omok.glue.game.session.model.messages.BoardSnapshotUpdate;
import teamnova.omok.glue.game.session.model.messages.GameCompletionNotice;
import teamnova.omok.glue.game.session.model.messages.PostGameDecisionPrompt;
import teamnova.omok.glue.game.session.model.messages.PostGameDecisionUpdate;
import teamnova.omok.glue.game.session.model.messages.PostGameResolution;
import teamnova.omok.glue.game.session.services.GameSessionRematchService;
import teamnova.omok.glue.game.session.states.GameStateHub;
import teamnova.omok.glue.game.session.states.event.DecisionTimeoutEvent;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContext;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContextService;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateType;
import teamnova.omok.modules.state_machine.interfaces.StateSignalListener;
import teamnova.omok.modules.state_machine.models.LifecycleEventKind;
import teamnova.omok.modules.state_machine.models.StateName;

/**
 * Handles post-game related outbound effects when entering relevant states.
 */
public final class PostGameSignalHandler implements StateSignalListener {
    private final GameSessionStateContext context;
    private final GameSessionStateContextService contextService;
    private final GameSessionServices services;
    private final GameStateHub hub;

    public PostGameSignalHandler(GameStateHub hub,
                                 GameSessionStateContext context,
                                 GameSessionStateContextService contextService,
                                 GameSessionServices services) {
        this.hub = hub;
        this.context = context;
        this.contextService = contextService;
        this.services = services;
    }

    @Override
    public Set<LifecycleEventKind> events() {
        return java.util.Set.of(LifecycleEventKind.ON_START, LifecycleEventKind.ON_UPDATE);
    }

    @Override
    public Set<StateName> states() {
        return java.util.Set.of(
            GameSessionStateType.POST_GAME_DECISION_WAITING.toStateName(),
            GameSessionStateType.POST_GAME_DECISION_RESOLVING.toStateName(),
            GameSessionStateType.COMPLETED.toStateName()
        );
    }

    @Override
    public void onSignal(StateName state, LifecycleEventKind kind) {
        GameSessionStateType type = GameSessionStateType.stateNameLookup(state);
        if (type == GameSessionStateType.POST_GAME_DECISION_WAITING) {
            if (kind == LifecycleEventKind.ON_START) {
                drainWaiting();
            }
            // ON_UPDATE: drain decision results (ACKs) and incremental updates
            if (kind == LifecycleEventKind.ON_UPDATE) {
                drainDecisionAcksAndUpdates();
            }
        } else if (type == GameSessionStateType.POST_GAME_DECISION_RESOLVING) {
            // All players have decided before deadline; ensure the decision timer is cancelled.
            if (kind == LifecycleEventKind.ON_START) {
                services.decisionTimeoutScheduler().cancel(context.session().sessionId());
                // Drain any remaining acks/updates before resolution broadcast
                drainDecisionAcksAndUpdates();
                drainResolving();
            }
        } else if (type == GameSessionStateType.COMPLETED) {
            if (kind == LifecycleEventKind.ON_START) {
                cancelAllTimers();
            }
        }
    }

    private void drainWaiting() {
        BoardSnapshotUpdate board = contextService.postGame().consumeBoardSnapshot(context);
        if (board != null) services.messenger().broadcastBoardSnapshot(context.session(), board);
        GameCompletionNotice notice = contextService.postGame().consumeGameCompletion(context);
        if (notice != null) services.messenger().broadcastGameCompleted(context.session());
        PostGameDecisionPrompt prompt = contextService.postGame().consumeDecisionPrompt(context);
        if (prompt != null) {
            services.messenger().broadcastPostGamePrompt(context.session(), prompt);
            // Schedule decision timeout at the recorded deadline to auto-resolve
            long deadline = contextService.postGame().decisionDeadline(context);
            if (deadline > 0) {
                var sessionId = context.session().sessionId();
                services.decisionTimeoutScheduler().schedule(sessionId, deadline, () -> {
                    try {
                        hub.submit(new DecisionTimeoutEvent(deadline));
                    } catch (Throwable ignored) { }
                });
            }
        }
        // Also drain any pending decision ACKs/updates queued during onEnter
        drainDecisionAcksAndUpdates();
    }

    private void drainDecisionAcksAndUpdates() {
        // Drain decision results: send ACK per requestId
        while (true) {
            var result = contextService.postGame().consumeDecisionResult(context);
            if (result == null) break;
            services.messenger().respondPostGameDecision(result.userId(), result.requestId(), result);
        }
        // Drain decision update snapshot if queued
        var update = contextService.postGame().consumeDecisionUpdate(context);
        if (update != null) {
            services.messenger().broadcastPostGameDecisionUpdate(context.session(), update);
        }
    }

    private void drainResolving() {
        PostGameDecisionUpdate update = contextService.postGame().consumeDecisionUpdate(context);
        if (update != null) services.messenger().broadcastPostGameDecisionUpdate(context.session(), update);
        PostGameResolution resolution = contextService.postGame().consumePostGameResolution(context);
        if (resolution != null) {
            switch (resolution.type()) {
                case REMATCH -> {
                    // Create a new session for rematch participants and notify clients
                    GameSessionRematchService.createAndBroadcast(
                        services,
                        context.session(),
                        resolution.rematchParticipants()
                    );
                }
                case TERMINATE -> {
                    // Announce termination and who is disconnected
                    services.messenger().broadcastSessionTerminated(context.session(), resolution.disconnected());
                }
            }
        }
    }

    private void cancelAllTimers() {
        services.turnTimeoutScheduler().cancel(context.session().sessionId());
        services.decisionTimeoutScheduler().cancel(context.session().sessionId());
    }
}
