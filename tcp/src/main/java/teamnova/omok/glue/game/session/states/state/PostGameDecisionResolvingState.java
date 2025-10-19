package teamnova.omok.glue.game.session.states.state;

import java.util.ArrayList;
import java.util.List;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionParticipantsAccess;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionPostGameAccess;
import teamnova.omok.glue.game.session.model.messages.PostGameResolution;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContext;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateType;
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
        List<String> rematch = new ArrayList<>(context.<GameSessionPostGameAccess>getSession().rematchRequestsView());
        List<String> disconnected = new ArrayList<>(context.<GameSessionParticipantsAccess>getSession().disconnectedUsersView());
        if (rematch.size() >= 2) {
            context.pendingPostGameResolution(
                PostGameResolution.rematch(context.getSession(), rematch, disconnected)
            );
            return StateStep.transition(GameSessionStateType.SESSION_REMATCH_PREPARING.toStateName());
        }
        context.pendingPostGameResolution(
            PostGameResolution.terminate(context.getSession(), disconnected)
        );
        return StateStep.transition(GameSessionStateType.SESSION_TERMINATING.toStateName());
    }
}
