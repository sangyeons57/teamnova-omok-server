package teamnova.omok.domain.session.game.entity.state.state;

import teamnova.omok.service.dto.TurnTimeoutResult;
import teamnova.omok.domain.session.game.entity.turn.TurnSnapshot;
import teamnova.omok.domain.session.game.entity.state.contract.GameSessionEvent;
import teamnova.omok.domain.session.game.entity.state.contract.GameSessionState;
import teamnova.omok.domain.session.game.entity.state.manage.GameSessionStateContext;
import teamnova.omok.domain.session.game.entity.state.manage.GameSessionStateStep;
import teamnova.omok.domain.session.game.entity.state.manage.GameSessionStateType;
import teamnova.omok.domain.session.game.entity.state.manage.TurnCycleContext;
import teamnova.omok.domain.session.game.GameSession;

/**
 * Waits for the current player input and forwards it through the turn pipeline.
 */
public class TurnWaitingState implements GameSessionState {
    @Override
    public GameSessionStateType type() {
        return GameSessionStateType.TURN_WAITING;
    }

    @Override
    public GameSessionStateStep onEvent(GameSessionStateContext context, GameSessionEvent event) {
        return switch (event) {
            case GameSessionEvent.Move ignored -> handleMove(context, (GameSessionEvent.Move) event);
            case GameSessionEvent.Timeout ignored -> handleTimeout(context, (GameSessionEvent.Timeout) event);
            default -> GameSessionStateStep.stay();
        };
    }

    private GameSessionStateStep handleMove(GameSessionStateContext context,
                                            GameSessionEvent.Move event) {
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
        GameSessionEvent.Timeout event) {

        GameSession session = context.session();
        TurnTimeoutResult result;

        session.lock().lock();
        try {
            if (!session.isGameStarted()) {
                result = TurnTimeoutResult.noop(session, null);
            } else {
                TurnSnapshot current = session.snapshotTurn();
                if (session.isGameFinished()) {
                    result = TurnTimeoutResult.noop(session, current);
                } else if (current.turnNumber() != event.expectedTurnNumber()) {
                    result = TurnTimeoutResult.noop(session, current);
                } else if (!session.getTurn().isExpired(event.timestamp())) {
                    result = TurnTimeoutResult.noop(session, current);
                } else {
                    String previousPlayerId = current.currentPlayerId();
                    TurnSnapshot next = session.advanceSkippingDisconnected(event.timestamp());
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
