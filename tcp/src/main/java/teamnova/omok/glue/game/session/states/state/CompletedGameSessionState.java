package teamnova.omok.glue.game.session.states.state;

import java.util.Objects;

import teamnova.omok.glue.game.session.interfaces.GameTurnService;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionParticipantsAccess;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionTurnAccess;
import teamnova.omok.glue.game.session.model.GameSession;
import teamnova.omok.glue.game.session.states.event.MoveEvent;
import teamnova.omok.glue.game.session.states.event.PostGameDecisionEvent;
import teamnova.omok.glue.game.session.states.event.ReadyEvent;
import teamnova.omok.glue.game.session.states.event.TimeoutEvent;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContext;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateType;
import teamnova.omok.glue.game.session.model.result.MoveResult;
import teamnova.omok.glue.game.session.model.result.MoveStatus;
import teamnova.omok.glue.game.session.model.result.PostGameDecisionResult;
import teamnova.omok.glue.game.session.model.result.PostGameDecisionStatus;
import teamnova.omok.glue.game.session.model.result.ReadyResult;
import teamnova.omok.glue.game.session.model.result.TurnTimeoutResult;
import teamnova.omok.modules.state_machine.interfaces.BaseEvent;
import teamnova.omok.modules.state_machine.interfaces.BaseState;
import teamnova.omok.modules.state_machine.interfaces.StateContext;
import teamnova.omok.modules.state_machine.models.StateName;
import teamnova.omok.modules.state_machine.models.StateStep;

/**
 * Terminal state once the session has concluded.
 */
public class CompletedGameSessionState implements BaseState {
    private final GameTurnService turnService;

    public CompletedGameSessionState(GameTurnService turnService) {
        this.turnService = Objects.requireNonNull(turnService, "turnService");
    }
    @Override
    public StateName name() {
        return GameSessionStateType.COMPLETED.toStateName();
    }

    @Override
    public <I extends StateContext> StateStep onEvent(I context, BaseEvent event) {
        GameSessionStateContext gameContext = (GameSessionStateContext) context;
        if (event instanceof ReadyEvent readyEvent) {
            return handleReady(gameContext, readyEvent);
        }
        if (event instanceof MoveEvent moveEvent) {
            return handleMove(gameContext, moveEvent);
        }
        if (event instanceof TimeoutEvent timeoutEvent) {
            return handleTimeout(gameContext, timeoutEvent);
        }
        if (event instanceof PostGameDecisionEvent decisionEvent) {
            return handlePostGameDecision(gameContext, decisionEvent);
        }
        return StateStep.stay();
    }

    private StateStep handleReady(GameSessionStateContext context,
                                  ReadyEvent event) {
        GameSessionParticipantsAccess session = context.getSession();
        if (!session.containsUser(event.userId())) {
            context.pendingReadyResult(ReadyResult.invalid(context.getSession(), event.userId()));
            return StateStep.stay();
        }
        boolean allReady = session.allReady();
        GameTurnService.TurnSnapshot snapshot =
            turnService.snapshot(context.<GameSessionTurnAccess>getSession());
        ReadyResult result = new ReadyResult(
            context.getSession(),
            true,
            false,
            allReady,
            false,
            snapshot,
            event.userId()
        );
        context.pendingReadyResult(result);
        return StateStep.stay();
    }

    private StateStep handleMove(GameSessionStateContext context,
                                 MoveEvent event) {
        GameSession session = context.getSession();
        if (session.playerIndexOf(event.userId()) < 0) {
            context.pendingMoveResult(
                MoveResult.invalid(
                    session,
                    MoveStatus.INVALID_PLAYER,
                    null,
                    event.userId(),
                    event.x(),
                    event.y()
                )
            );
            return StateStep.stay();
        }
        GameTurnService.TurnSnapshot snapshot =
            turnService.snapshot(context.getSession());
        context.pendingMoveResult(MoveResult.invalid(
            session,
            MoveStatus.GAME_FINISHED,
            snapshot,
            event.userId(),
            event.x(),
            event.y()
        ));
        return StateStep.stay();
    }

    private StateStep handleTimeout(GameSessionStateContext context,
                                    TimeoutEvent event) {
        GameSession session = context.getSession();
        GameTurnService.TurnSnapshot snapshot =
            turnService.snapshot(context.getSession());
        context.pendingTimeoutResult(
            TurnTimeoutResult.noop(session, snapshot)
        );
        return StateStep.stay();
    }

    private StateStep handlePostGameDecision(GameSessionStateContext context,
                                             PostGameDecisionEvent event) {
        GameSession session = context.getSession();
        context.pendingDecisionResult(
            PostGameDecisionResult.rejected(
                session,
                event.userId(),
                PostGameDecisionStatus.SESSION_CLOSED
            )
        );
        return StateStep.stay();
    }
}
