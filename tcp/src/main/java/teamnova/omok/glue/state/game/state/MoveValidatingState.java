package teamnova.omok.glue.state.game.state;

import teamnova.omok.glue.service.TurnService;
import teamnova.omok.glue.service.dto.MoveResult;
import teamnova.omok.glue.service.dto.MoveStatus;
import teamnova.omok.glue.state.game.manage.GameSessionStateContext;
import teamnova.omok.glue.state.game.manage.GameSessionStateType;
import teamnova.omok.glue.state.game.manage.TurnCycleContext;
import teamnova.omok.glue.store.GameSession;
import teamnova.omok.glue.store.Stone;
import teamnova.omok.modules.state_machine.interfaces.BaseState;
import teamnova.omok.modules.state_machine.interfaces.StateContext;
import teamnova.omok.modules.state_machine.models.StateName;
import teamnova.omok.modules.state_machine.models.StateStep;

/**
 * Validates whether a move request can proceed.
 */
public final class MoveValidatingState implements BaseState {
    @Override
    public StateName name() {
        return GameSessionStateType.MOVE_VALIDATING.toStateName();
    }

    @Override
    public <I extends StateContext> StateStep onEnter(I context) {
        return onEnterInternal((GameSessionStateContext) context);
    }

    private StateStep onEnterInternal(GameSessionStateContext context) {
        TurnCycleContext cycle = context.activeTurnCycle();
        if (cycle == null) {
            return StateStep.transition(GameSessionStateType.TURN_WAITING.toStateName());
        }
        GameSession session = cycle.session();
        String userId = cycle.userId();
        int x = cycle.x();
        int y = cycle.y();

        int playerIndex = session.playerIndexOf(userId);
        if (playerIndex < 0) {
            invalidate(context, MoveResult.invalid(
                session,
                MoveStatus.INVALID_PLAYER,
                null,
                userId,
                x,
                y
            ));
            return StateStep.transition(GameSessionStateType.TURN_WAITING.toStateName());
        }
        cycle.playerIndex(playerIndex);

        if (!session.isGameStarted()) {
            invalidate(context, MoveResult.invalid(
                session,
                MoveStatus.GAME_NOT_STARTED,
                null,
                userId,
                x,
                y
            ));
            return StateStep.transition(GameSessionStateType.TURN_WAITING.toStateName());
        }

        TurnService.TurnSnapshot currentSnapshot =
            context.turnService().snapshot(session.getTurnStore(), session.getUserIds());
        cycle.snapshots().current(currentSnapshot);

        if (session.isGameFinished()) {
            invalidate(context, MoveResult.invalid(
                session,
                MoveStatus.GAME_FINISHED,
                currentSnapshot,
                userId,
                x,
                y
            ));
            return StateStep.transition(GameSessionStateType.TURN_WAITING.toStateName());
        }

        if (!context.boardService().isWithinBounds(session.getBoardStore(), x, y)) {
            invalidate(context, MoveResult.invalid(
                session,
                MoveStatus.OUT_OF_BOUNDS,
                currentSnapshot,
                userId,
                x,
                y
            ));
            return StateStep.transition(GameSessionStateType.TURN_WAITING.toStateName());
        }

        Integer currentIndex = context.turnService().currentPlayerIndex(session.getTurnStore());
        if (currentIndex == null || currentIndex != playerIndex) {
            invalidate(context, MoveResult.invalid(
                session,
                MoveStatus.OUT_OF_TURN,
                currentSnapshot,
                userId,
                x,
                y
            ));
            return StateStep.transition(GameSessionStateType.TURN_WAITING.toStateName());
        }

        if (!context.boardService().isEmpty(session.getBoardStore(), x, y)) {
            invalidate(context, MoveResult.invalid(
                session,
                MoveStatus.CELL_OCCUPIED,
                currentSnapshot,
                userId,
                x,
                y
            ));
            return StateStep.transition(GameSessionStateType.TURN_WAITING.toStateName());
        }

        Stone stone = Stone.fromPlayerOrder(playerIndex);
        cycle.stone(stone);
        return StateStep.transition(GameSessionStateType.MOVE_APPLYING.toStateName());
    }

    private void invalidate(GameSessionStateContext context, MoveResult result) {
        context.pendingMoveResult(result);
        context.clearTurnCycle();
    }
}
