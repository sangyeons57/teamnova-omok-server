package teamnova.omok.glue.game.session.states.signal;

import java.util.Set;

import teamnova.omok.glue.game.session.model.messages.BoardSnapshotUpdate;
import teamnova.omok.glue.game.session.model.messages.GameCompletionNotice;
import teamnova.omok.glue.game.session.model.messages.PostGameDecisionPrompt;
import teamnova.omok.glue.game.session.model.messages.PostGameDecisionUpdate;
import teamnova.omok.glue.game.session.model.messages.PostGameResolution;
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
    private final teamnova.omok.glue.game.session.model.dto.GameSessionServices services;

    public PostGameSignalHandler(GameSessionStateContext context,
                                 GameSessionStateContextService contextService,
                                 teamnova.omok.glue.game.session.model.dto.GameSessionServices services) {
        this.context = context;
        this.contextService = contextService;
        this.services = services;
    }

    @Override
    public Set<LifecycleEventKind> events() {
        return java.util.Set.of(LifecycleEventKind.ON_START);
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
            drainWaiting();
        } else if (type == GameSessionStateType.POST_GAME_DECISION_RESOLVING) {
            drainResolving();
        } else if (type == GameSessionStateType.COMPLETED) {
            cancelAllTimers();
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
            // Decision timeout scheduling can be added here if a repository/runtime is injectable
        }
    }

    private void drainResolving() {
        PostGameDecisionUpdate update = contextService.postGame().consumeDecisionUpdate(context);
        if (update != null) services.messenger().broadcastPostGameDecisionUpdate(context.session(), update);
        PostGameResolution resolution = contextService.postGame().consumePostGameResolution(context);
        if (resolution != null) {
            // Resolution side-effects (rematch/session termination) are performed within state transitions.
        }
    }

    private void cancelAllTimers() {
        services.turnTimeoutScheduler().cancel(context.session().sessionId());
        services.decisionTimeoutScheduler().cancel(context.session().sessionId());
    }
}
