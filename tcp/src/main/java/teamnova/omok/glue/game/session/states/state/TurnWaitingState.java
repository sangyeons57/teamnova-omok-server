package teamnova.omok.glue.game.session.states.state;

import java.util.Objects;

import teamnova.omok.glue.game.session.interfaces.GameTurnService;
import teamnova.omok.glue.game.session.model.GameSession;
import teamnova.omok.glue.game.session.model.result.TurnTimeoutResult;
import teamnova.omok.glue.game.session.states.event.MoveEvent;
import teamnova.omok.glue.game.session.states.event.TimeoutEvent;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContext;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContextService;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateType;
import teamnova.omok.glue.game.session.states.manage.TurnCycleContext;
import teamnova.omok.modules.state_machine.interfaces.BaseEvent;
import teamnova.omok.modules.state_machine.interfaces.BaseState;
import teamnova.omok.modules.state_machine.interfaces.StateContext;
import teamnova.omok.modules.state_machine.models.StateName;
import teamnova.omok.modules.state_machine.models.StateStep;

/**
 * Waits for the current player input and forwards it through the turn pipeline.
 */
public class TurnWaitingState implements BaseState {
    private final GameSessionStateContextService contextService;
    private final GameTurnService turnService;

    public TurnWaitingState(GameSessionStateContextService contextService,
                            GameTurnService turnService) {
        this.contextService = Objects.requireNonNull(contextService, "contextService");
        this.turnService = Objects.requireNonNull(turnService, "turnService");
    }
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
        if (contextService.turn().activeTurnCycle(context) != null) {
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
            contextService.turn().beginTurnCycle(context, cycleContext);
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
            if (!context.lifecycle().isGameStarted()) {
                result = TurnTimeoutResult.noop(session, null);
            } else {
                GameTurnService.TurnSnapshot current =
                    turnService.snapshot(context.turns());
                if (context.outcomes().isGameFinished()) {
                    result = TurnTimeoutResult.noop(session, current);
                } else if (current.turnNumber() != event.expectedTurnNumber()) {
                    result = TurnTimeoutResult.noop(session, current);
                } else if (!turnService.isExpired(context.turns(), event.timestamp())) {
                    result = TurnTimeoutResult.noop(session, current);
                } else {
                    String previousPlayerId = current.currentPlayerId();
                    GameTurnService.TurnSnapshot next = turnService
                        .advanceSkippingDisconnected(
                            context.turns(),
                            context.participants().disconnectedUsersView(),
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

        contextService.turn().queueTimeoutResult(context, result);
        if (context.outcomes().isGameFinished()) {
            return StateStep.transition(GameSessionStateType.COMPLETED.toStateName());
        }
        return StateStep.stay();
    }
}
