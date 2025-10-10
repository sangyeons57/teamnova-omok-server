package teamnova.omok.state.game.state;

import teamnova.omok.service.InGameSessionService;
import teamnova.omok.service.TurnService;
import teamnova.omok.state.game.contract.GameSessionState;
import teamnova.omok.state.game.event.GameSessionEventRegistry;
import teamnova.omok.state.game.event.GameSessionEventType;
import teamnova.omok.state.game.event.MoveEvent;
import teamnova.omok.state.game.event.PostGameDecisionEvent;
import teamnova.omok.state.game.event.ReadyEvent;
import teamnova.omok.state.game.event.TimeoutEvent;
import teamnova.omok.state.game.manage.GameSessionStateContext;
import teamnova.omok.state.game.manage.GameSessionStateStep;
import teamnova.omok.state.game.manage.GameSessionStateType;
import teamnova.omok.store.GameSession;

/**
 * Terminal state once the session has concluded.
 */
public class CompletedGameSessionState implements GameSessionState {
    @Override
    public GameSessionStateType type() {
        return GameSessionStateType.COMPLETED;
    }

    @Override
    public void registerHandlers(GameSessionEventRegistry registry) {
        registry.register(GameSessionEventType.READY, ReadyEvent.class, this::handleReady);
        registry.register(GameSessionEventType.MOVE, MoveEvent.class, this::handleMove);
        registry.register(GameSessionEventType.TIMEOUT, TimeoutEvent.class, this::handleTimeout);
        registry.register(GameSessionEventType.POST_GAME_DECISION, PostGameDecisionEvent.class, this::handlePostGameDecision);
    }

    private GameSessionStateStep handleReady(GameSessionStateContext context,
                                             ReadyEvent event) {
        GameSession session = context.session();
        if (!session.containsUser(event.userId())) {
            context.pendingReadyResult(InGameSessionService.ReadyResult.invalid(session, event.userId()));
            return GameSessionStateStep.stay();
        }
        boolean allReady = session.allReady();
        TurnService.TurnSnapshot snapshot =
            context.turnService().snapshot(session.getTurnStore(), session.getUserIds());
        InGameSessionService.ReadyResult result = new InGameSessionService.ReadyResult(
            session,
            true,
            false,
            allReady,
            false,
            snapshot,
            event.userId()
        );
        context.pendingReadyResult(result);
        return GameSessionStateStep.stay();
    }

    private GameSessionStateStep handleMove(GameSessionStateContext context,
                                            MoveEvent event) {
        GameSession session = context.session();
        if (session.playerIndexOf(event.userId()) < 0) {
            context.pendingMoveResult(
                InGameSessionService.MoveResult.invalid(
                    session,
                    InGameSessionService.MoveStatus.INVALID_PLAYER,
                    null,
                    event.userId(),
                    event.x(),
                    event.y()
                )
            );
            return GameSessionStateStep.stay();
        }
        TurnService.TurnSnapshot snapshot =
            context.turnService().snapshot(session.getTurnStore(), session.getUserIds());
        context.pendingMoveResult(InGameSessionService.MoveResult.invalid(
            session,
            InGameSessionService.MoveStatus.GAME_FINISHED,
            snapshot,
            event.userId(),
            event.x(),
            event.y()
        ));
        return GameSessionStateStep.stay();
    }

    private GameSessionStateStep handleTimeout(GameSessionStateContext context,
                                               TimeoutEvent event) {
        GameSession session = context.session();
        TurnService.TurnSnapshot snapshot =
            context.turnService().snapshot(session.getTurnStore(), session.getUserIds());
        context.pendingTimeoutResult(
            InGameSessionService.TurnTimeoutResult.noop(session, snapshot)
        );
        return GameSessionStateStep.stay();
    }

    private GameSessionStateStep handlePostGameDecision(GameSessionStateContext context,
                                                        PostGameDecisionEvent event) {
        GameSession session = context.session();
        context.pendingDecisionResult(
            InGameSessionService.PostGameDecisionResult.rejected(
                session,
                event.userId(),
                InGameSessionService.PostGameDecisionStatus.SESSION_CLOSED
            )
        );
        return GameSessionStateStep.stay();
    }
}
