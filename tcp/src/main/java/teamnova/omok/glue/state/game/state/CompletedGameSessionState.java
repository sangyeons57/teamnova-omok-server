package teamnova.omok.glue.state.game.state;

import teamnova.omok.glue.service.TurnService;
import teamnova.omok.glue.service.dto.MoveResult;
import teamnova.omok.glue.service.dto.MoveStatus;
import teamnova.omok.glue.service.dto.PostGameDecisionResult;
import teamnova.omok.glue.service.dto.PostGameDecisionStatus;
import teamnova.omok.glue.service.dto.ReadyResult;
import teamnova.omok.glue.service.dto.TurnTimeoutResult;
import teamnova.omok.glue.state.game.event.MoveEvent;
import teamnova.omok.glue.state.game.event.PostGameDecisionEvent;
import teamnova.omok.glue.state.game.event.ReadyEvent;
import teamnova.omok.glue.state.game.event.TimeoutEvent;
import teamnova.omok.glue.state.game.manage.GameSessionStateContext;
import teamnova.omok.glue.state.game.manage.GameSessionStateType;
import teamnova.omok.glue.store.GameSession;
import teamnova.omok.modules.state_machine.interfaces.BaseEvent;
import teamnova.omok.modules.state_machine.interfaces.BaseState;
import teamnova.omok.modules.state_machine.interfaces.StateContext;
import teamnova.omok.modules.state_machine.models.StateName;
import teamnova.omok.modules.state_machine.models.StateStep;

/**
 * Terminal state once the session has concluded.
 */
public class CompletedGameSessionState implements BaseState {
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
        GameSession session = context.session();
        if (!session.containsUser(event.userId())) {
            context.pendingReadyResult(ReadyResult.invalid(session, event.userId()));
            return StateStep.stay();
        }
        boolean allReady = session.allReady();
        TurnService.TurnSnapshot snapshot =
            context.turnService().snapshot(session.getTurnStore(), session.getUserIds());
        ReadyResult result = new ReadyResult(
            session,
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
        GameSession session = context.session();
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
        TurnService.TurnSnapshot snapshot =
            context.turnService().snapshot(session.getTurnStore(), session.getUserIds());
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
        GameSession session = context.session();
        TurnService.TurnSnapshot snapshot =
            context.turnService().snapshot(session.getTurnStore(), session.getUserIds());
        context.pendingTimeoutResult(
            TurnTimeoutResult.noop(session, snapshot)
        );
        return StateStep.stay();
    }

    private StateStep handlePostGameDecision(GameSessionStateContext context,
                                             PostGameDecisionEvent event) {
        GameSession session = context.session();
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
