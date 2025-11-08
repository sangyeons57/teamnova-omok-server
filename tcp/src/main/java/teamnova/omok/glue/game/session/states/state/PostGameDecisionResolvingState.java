package teamnova.omok.glue.game.session.states.state;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import teamnova.omok.glue.game.session.model.messages.PostGameResolution;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContext;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContextService;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateType;
import teamnova.omok.modules.state_machine.interfaces.BaseState;
import teamnova.omok.modules.state_machine.interfaces.StateContext;
import teamnova.omok.modules.state_machine.models.StateName;
import teamnova.omok.modules.state_machine.models.StateStep;

/**
 * Aggregates post-game decisions and chooses the next terminal path.
 */
public final class PostGameDecisionResolvingState implements BaseState {
    private final GameSessionStateContextService contextService;

    public PostGameDecisionResolvingState(GameSessionStateContextService contextService) {
        this.contextService = Objects.requireNonNull(contextService, "contextService");
    }
    @Override
    public StateName name() {
        return GameSessionStateType.POST_GAME_DECISION_RESOLVING.toStateName();
    }

    @Override
    public <I extends StateContext> StateStep onEnter(I context) {
        return onEnterInternal((GameSessionStateContext) context);
    }

    private StateStep onEnterInternal(GameSessionStateContext context) {
        System.out.println("[SESSION][" + context.session().sessionId() + "] PostGameDecisionResolvingState");

        List<String> rematch = new ArrayList<>(context.postGame().rematchRequestsView());
        List<String> disconnected = new ArrayList<>(context.participants().disconnectedUsersView());
        if (rematch.size() >= 2) {
            contextService.postGame().queuePostGameResolution(
                context,
                PostGameResolution.rematch(rematch, disconnected)
            );
            return StateStep.transition(GameSessionStateType.SESSION_REMATCH_PREPARING.toStateName());
        } else {
            contextService.postGame().queuePostGameResolution(
                context,
                PostGameResolution.terminate(disconnected)
            );
            return StateStep.transition(GameSessionStateType.SESSION_TERMINATING.toStateName());
        }
    }
}
