package teamnova.omok.domain.session.game.entity.state.state;

import java.util.ArrayList;
import java.util.List;

import teamnova.omok.service.dto.PostGameResolution;
import teamnova.omok.domain.session.game.entity.state.contract.GameSessionState;
import teamnova.omok.domain.session.game.entity.state.manage.GameSessionStateContext;
import teamnova.omok.domain.session.game.entity.state.manage.GameSessionStateStep;
import teamnova.omok.domain.session.game.entity.state.manage.GameSessionStateType;
import teamnova.omok.domain.session.game.GameSession;

/**
 * Aggregates post-game decisions and chooses the next terminal path.
 */
public final class PostGameDecisionResolvingState implements GameSessionState {
    @Override
    public GameSessionStateType type() {
        return GameSessionStateType.POST_GAME_DECISION_RESOLVING;
    }

    @Override
    public GameSessionStateStep onEnter(GameSessionStateContext context) {
        GameSession session = context.session();
        List<String> rematch = new ArrayList<>(session.rematchRequestsView());
        List<String> disconnected = new ArrayList<>(session.disconnectedUsersView());
        if (rematch.size() >= 2) {
            context.pendingPostGameResolution(
                PostGameResolution.rematch(session, rematch, disconnected)
            );
            return GameSessionStateStep.transition(GameSessionStateType.SESSION_REMATCH_PREPARING);
        }
        context.pendingPostGameResolution(
            PostGameResolution.terminate(session, disconnected)
        );
        return GameSessionStateStep.transition(GameSessionStateType.SESSION_TERMINATING);
    }
}
