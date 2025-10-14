package teamnova.omok.glue.state.game.state;

import teamnova.omok.glue.service.TurnService;
import teamnova.omok.glue.service.dto.TurnTimeoutResult;
import teamnova.omok.glue.state.game.event.MoveEvent;
import teamnova.omok.glue.state.game.event.TimeoutEvent;
import teamnova.omok.glue.state.game.manage.GameSessionStateContext;
import teamnova.omok.glue.state.game.manage.GameSessionStateType;
import teamnova.omok.glue.state.game.manage.TurnCycleContext;
import teamnova.omok.glue.store.GameSession;
import teamnova.omok.modules.state_machine.interfaces.BaseEvent;
import teamnova.omok.modules.state_machine.interfaces.BaseState;
import teamnova.omok.modules.state_machine.interfaces.StateContext;
import teamnova.omok.modules.state_machine.models.StateName;
import teamnova.omok.modules.state_machine.models.StateStep;

/**
 * Waits for the current player input and forwards it through the turn pipeline.
 */
public class TurnWaitingState implements BaseState {
    @Override
    public StateName name() {
        return GameSessionStateType.TURN_WAITING.toStateName();
    }

    @Override
    public <I extends StateContext> StateStep onEvent(I context, BaseEvent event) {
        GameSessionStateContext gameContext = (GameSessionStateContext) context;
        if (event instanceof MoveEvent moveEvent) {
            return handleMove(gameContext, moveEvent);
        }
        if (event instanceof TimeoutEvent timeoutEvent) {
            return handleTimeout(gameContext, timeoutEvent);
        }
        return StateStep.stay();
    }

    private StateStep handleMove(GameSessionStateContext context,
                                 MoveEvent event) {
        if (context.activeTurnCycle() != null) {
            return StateStep.stay();
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
            return StateStep.transition(GameSessionStateType.MOVE_VALIDATING.toStateName());
        } finally {
            session.lock().unlock();
        }
    }

    private StateStep handleTimeout(GameSessionStateContext context,
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
            return StateStep.transition(GameSessionStateType.COMPLETED.toStateName());
        }
        return StateStep.stay();
    }
}
