package teamnova.omok.glue.state.game.state;

import java.util.ArrayList;
import java.util.List;

import teamnova.omok.glue.service.dto.PostGameResolution;
import teamnova.omok.glue.state.game.manage.GameSessionStateContext;
import teamnova.omok.glue.state.game.manage.GameSessionStateType;
import teamnova.omok.modules.state_machine.interfaces.BaseState;
import teamnova.omok.modules.state_machine.interfaces.StateContext;
import teamnova.omok.modules.state_machine.models.StateName;
import teamnova.omok.modules.state_machine.models.StateStep;

/**
 * Aggregates post-game decisions and chooses the next terminal path.
 */
public final class PostGameDecisionResolvingState implements BaseState {
    @Override
    public StateName name() {
        return GameSessionStateType.POST_GAME_DECISION_RESOLVING.toStateName();
    }

    @Override
    public <I extends StateContext> StateStep onEnter(I context) {
        return onEnterInternal((GameSessionStateContext) context);
    }

    private StateStep onEnterInternal(GameSessionStateContext context) {
        List<String> rematch = new ArrayList<>(context.session().rematchRequestsView());
        List<String> disconnected = new ArrayList<>(context.session().disconnectedUsersView());
        if (rematch.size() >= 2) {
            context.pendingPostGameResolution(
                PostGameResolution.rematch(context.session(), rematch, disconnected)
            );
            return StateStep.transition(GameSessionStateType.SESSION_REMATCH_PREPARING.toStateName());
        }
        context.pendingPostGameResolution(
            PostGameResolution.terminate(context.session(), disconnected)
        );
        return StateStep.transition(GameSessionStateType.SESSION_TERMINATING.toStateName());
    }
}
