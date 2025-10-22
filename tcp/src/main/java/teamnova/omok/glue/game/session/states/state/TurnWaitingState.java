package teamnova.omok.glue.game.session.states.state;

import java.util.Objects;

import teamnova.omok.glue.game.session.interfaces.GameTurnService;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionAccess;
import teamnova.omok.glue.game.session.log.GameSessionLogger;
import teamnova.omok.glue.game.session.model.dto.TurnSnapshot;
import teamnova.omok.glue.game.session.model.runtime.TurnPersonalFrame;
import teamnova.omok.glue.game.session.states.event.MoveEvent;
import teamnova.omok.glue.game.session.states.event.TimeoutEvent;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContext;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContextService;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateType;
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
        TurnPersonalFrame frame = contextService.turn().currentPersonalTurn(context);
        if (frame == null) {
            GameSessionLogger.event(context, GameSessionStateType.TURN_WAITING, "MoveEvent:ignored",
                "reason=no-active-frame",
                String.format("user=%s x=%d y=%d", event.userId(), event.x(), event.y()));
            return StateStep.stay();
        }
        if (frame.hasActiveMove()) {
            GameSessionLogger.event(context, GameSessionStateType.TURN_WAITING, "MoveEvent:ignored",
                "reason=move-in-progress",
                String.format("user=%s x=%d y=%d", event.userId(), event.x(), event.y()));
            return StateStep.stay();
        }
        GameSessionAccess session = context.session();
        session.lock().lock();
        try {
            contextService.turn().beginTurnCycle(
                context,
                event.userId(),
                event.x(),
                event.y(),
                event.timestamp()
            );
            GameSessionLogger.event(context, GameSessionStateType.TURN_WAITING, "MoveEvent:accepted",
                String.format("user=%s x=%d y=%d", event.userId(), event.x(), event.y()));
            return StateStep.transition(GameSessionStateType.MOVE_VALIDATING.toStateName());
        } finally {
            session.lock().unlock();
        }
    }

    private StateStep handleTimeout(GameSessionStateContext context,
                                    TimeoutEvent event) {
        GameSessionAccess session = context.session();
        TurnPersonalFrame frame = contextService.turn().currentPersonalTurn(context);
        TurnSnapshot currentSnapshot = null;
        TurnSnapshot nextSnapshot = null;
        String previousPlayerId = null;
        boolean timedOut = false;

        boolean advanced = false;
        session.lock().lock();
        try {
            if (context.lifecycle().isGameStarted()) {
                currentSnapshot = turnService.snapshot(context.turns());
                if (context.outcomes().isGameFinished()) {
                    timedOut = false;
                } else if (currentSnapshot == null || currentSnapshot.turnNumber() != event.expectedTurnNumber()) {
                    timedOut = false;
                } else if (!turnService.isExpired(context.turns(), event.timestamp())) {
                    timedOut = false;
                } else {
                    timedOut = true;
                    previousPlayerId = currentSnapshot.currentPlayerId();
                    nextSnapshot = turnService
                        .advanceSkippingDisconnected(
                            context.turns(),
                            context.participants().disconnectedUsersView(),
                            event.timestamp()
                        );
                    contextService.turn().recordTurnSnapshot(context, nextSnapshot, event.timestamp());
                    advanced = nextSnapshot != null;
                }
            }
        } finally {
            session.lock().unlock();
        }

        if (frame != null) {
            TurnSnapshot outcomeSnapshot = timedOut
                ? (nextSnapshot != null ? nextSnapshot : currentSnapshot)
                : currentSnapshot;
            if (timedOut || outcomeSnapshot != null) {
                contextService.turn().recordTimeoutOutcome(
                    context,
                    timedOut,
                    outcomeSnapshot,
                    previousPlayerId
                );
            }
        }
        GameSessionLogger.event(context, GameSessionStateType.TURN_WAITING, "TimeoutProcessed",
            String.format("timedOut=%s previous=%s", timedOut, previousPlayerId),
            currentSnapshot == null ? "currentSnapshot=null"
                : String.format("currentTurn=%s", currentSnapshot.currentPlayerId()),
            nextSnapshot == null ? "nextSnapshot=null"
                : String.format("nextTurn=%s wrapped=%s", nextSnapshot.currentPlayerId(), nextSnapshot.wrapped()));
        if (context.outcomes().isGameFinished()) {
            contextService.turn().consumeTurnSnapshot(context);
            return StateStep.transition(GameSessionStateType.COMPLETED.toStateName());
        }
        if (!advanced) {
            return StateStep.stay();
        }
        TurnSnapshot snapshot = contextService.turn().peekTurnSnapshot(context);
        GameSessionStateType nextState = (snapshot != null && snapshot.wrapped())
            ? GameSessionStateType.TURN_END
            : GameSessionStateType.TURN_PERSONAL_START;
        return StateStep.transition(nextState.toStateName());
    }
}
