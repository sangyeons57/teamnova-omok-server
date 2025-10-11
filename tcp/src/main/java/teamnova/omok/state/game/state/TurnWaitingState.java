package teamnova.omok.state.game.state;

import teamnova.omok.service.TurnService;
import teamnova.omok.service.dto.TurnTimeoutResult;
import teamnova.omok.state.game.contract.GameSessionState;
import teamnova.omok.state.game.event.GameSessionEventRegistry;
import teamnova.omok.state.game.event.GameSessionEventType;
import teamnova.omok.state.game.event.MoveEvent;
import teamnova.omok.state.game.event.TimeoutEvent;
import teamnova.omok.state.game.manage.GameSessionStateContext;
import teamnova.omok.state.game.manage.GameSessionStateStep;
import teamnova.omok.state.game.manage.GameSessionStateType;
import teamnova.omok.state.game.manage.TurnCycleContext;
import teamnova.omok.store.GameSession;

/**
 * Waits for the current player input and forwards it through the turn pipeline.
 */
public class TurnWaitingState implements GameSessionState {
    @Override
    public GameSessionStateType type() {
        return GameSessionStateType.TURN_WAITING;
    }

    @Override
    public void registerHandlers(GameSessionEventRegistry registry) {
        registry.register(GameSessionEventType.MOVE, MoveEvent.class, this::handleMove);
        registry.register(GameSessionEventType.TIMEOUT, TimeoutEvent.class, this::handleTimeout);
    }

    private GameSessionStateStep handleMove(GameSessionStateContext context,
                                            MoveEvent event) {
        if (context.activeTurnCycle() != null) {
            return GameSessionStateStep.stay();
        }
        GameSession session = context.session();
        session.lock().lock();
        try {
            TurnCycleContext cycleContext = new TurnCycleContext(
                context,
                event.userId(),
                event.x(),
                event.y(),
                event.timestamp()
            );
            context.beginTurnCycle(cycleContext);
            return GameSessionStateStep.transition(GameSessionStateType.MOVE_VALIDATING);
        } finally {
            session.lock().unlock();
        }
    }

    private GameSessionStateStep handleTimeout(
        GameSessionStateContext context,
        TimeoutEvent event) {
        GameSession session = context.session();
        TurnTimeoutResult result;

        session.lock().lock();
        try {
            if (!session.isGameStarted()) {
                result = TurnTimeoutResult.noop(session, null);
            } else {
                TurnService.TurnSnapshot current =
                    context.turnService().snapshot(session.getTurnStore(), session.getUserIds());
                if (session.isGameFinished()) {
                    result = TurnTimeoutResult.noop(session, current);
                } else if (current.turnNumber() != event.expectedTurnNumber()) {
                    result = TurnTimeoutResult.noop(session, current);
                } else if (!context.turnService().isExpired(session.getTurnStore(), event.timestamp())) {
                    result = TurnTimeoutResult.noop(session, current);
                } else {
                    String previousPlayerId = current.currentPlayerId();
                    TurnService.TurnSnapshot next = context.turnService()
                        .advanceSkippingDisconnected(
                            session.getTurnStore(),
                            session.getUserIds(),
                            session.disconnectedUsersView(),
                            event.timestamp()
                        );
                    result = TurnTimeoutResult.timedOut(
                        session,
                        current,
                        next,
                        previousPlayerId
                    );
                }
            }
        } finally {
            session.lock().unlock();
        }

        context.pendingTimeoutResult(result);
        if (session.isGameFinished()) {
            return GameSessionStateStep.transition(GameSessionStateType.COMPLETED);
        }
        return GameSessionStateStep.stay();
    }
}
