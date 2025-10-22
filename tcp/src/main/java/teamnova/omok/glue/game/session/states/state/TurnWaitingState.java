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
        // Validate that the event user is the current player
        TurnSnapshot snapshot = turnService.snapshot(context.turns());
        if (snapshot == null || !event.userId().equals(snapshot.currentPlayerId())) {
            GameSessionLogger.event(context, GameSessionStateType.TURN_WAITING, "MoveEvent:ignored",
                "reason=not-current-player",
                snapshot == null
                    ? "snapshot=null"
                    : String.format("user=%s expected=%s", event.userId(), snapshot.currentPlayerId()));
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
                event.timestamp(),
                event.requestId()
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
        if (frame == null) {
            GameSessionLogger.event(context, GameSessionStateType.TURN_WAITING, "TimeoutIgnored",
                "reason=no-active-frame",
                "expectedTurn=" + event.expectedTurnNumber());
            return StateStep.stay();
        }
        TurnSnapshot currentSnapshot = null;
        boolean timedOut = false;
        boolean gameFinished = context.outcomes().isGameFinished();

        session.lock().lock();
        try {
            if (context.lifecycle().isGameStarted() && !gameFinished) {
                currentSnapshot = turnService.snapshot(context.turns());
                if (currentSnapshot != null
                    && currentSnapshot.turnNumber() == event.expectedTurnNumber()
                    && turnService.isExpired(context.turns(), event.timestamp())) {
                    timedOut = true;
                    frame.currentSnapshot(currentSnapshot);
                }
            }
        } finally {
            session.lock().unlock();
        }

        if (!timedOut) {
            GameSessionLogger.event(context, GameSessionStateType.TURN_WAITING, "TimeoutIgnored",
                "reason=stale-or-not-expired",
                currentSnapshot == null ? "currentSnapshot=null"
                    : String.format("currentTurn=%s expectedTurn=%d",
                        currentSnapshot.currentPlayerId(), event.expectedTurnNumber()));
            if (currentSnapshot != null) {
                contextService.turn().recordTimeoutOutcome(
                    context,
                    false,
                    currentSnapshot,
                    event.timestamp()
                );
            }
            return StateStep.stay();
        }

        contextService.turn().recordTimeoutOutcome(
            context,
            true,
            currentSnapshot,
            event.timestamp()
        );
        return StateStep.transition(GameSessionStateType.TURN_PERSONAL_END.toStateName());
    }
}
