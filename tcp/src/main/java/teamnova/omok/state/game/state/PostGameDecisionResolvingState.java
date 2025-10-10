package teamnova.omok.state.game.state;

import java.util.ArrayList;
import java.util.List;

import teamnova.omok.service.InGameSessionService;
import teamnova.omok.state.game.contract.GameSessionState;
import teamnova.omok.state.game.event.GameSessionEventRegistry;
import teamnova.omok.state.game.manage.GameSessionStateContext;
import teamnova.omok.state.game.manage.GameSessionStateStep;
import teamnova.omok.state.game.manage.GameSessionStateType;
import teamnova.omok.store.GameSession;

/**
 * Aggregates post-game decisions and chooses the next terminal path.
 */
public final class PostGameDecisionResolvingState implements GameSessionState {
    @Override
    public GameSessionStateType type() {
        return GameSessionStateType.POST_GAME_DECISION_RESOLVING;
    }

    @Override
    public void registerHandlers(GameSessionEventRegistry registry) {
        // no direct event handling; progression happens immediately on enter
    }

    @Override
    public GameSessionStateStep onEnter(GameSessionStateContext context) {
        GameSession session = context.session();
        List<String> rematch = new ArrayList<>(session.rematchRequestsView());
        List<String> disconnected = new ArrayList<>(session.disconnectedUsersView());
        if (rematch.size() >= 2) {
            context.pendingPostGameResolution(
                InGameSessionService.PostGameResolution.rematch(session, rematch, disconnected)
            );
            return GameSessionStateStep.transition(GameSessionStateType.SESSION_REMATCH_PREPARING);
        }
        context.pendingPostGameResolution(
            InGameSessionService.PostGameResolution.terminate(session, disconnected)
        );
        return GameSessionStateStep.transition(GameSessionStateType.SESSION_TERMINATING);
    }
}
