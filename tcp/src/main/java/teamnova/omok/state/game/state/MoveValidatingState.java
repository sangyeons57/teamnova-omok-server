package teamnova.omok.state.game.state;

import teamnova.omok.service.TurnService;
import teamnova.omok.service.dto.MoveResult;
import teamnova.omok.service.dto.MoveStatus;
import teamnova.omok.state.game.contract.GameSessionState;
import teamnova.omok.state.game.manage.GameSessionStateContext;
import teamnova.omok.state.game.manage.GameSessionStateStep;
import teamnova.omok.state.game.manage.GameSessionStateType;
import teamnova.omok.state.game.manage.TurnCycleContext;
import teamnova.omok.store.GameSession;
import teamnova.omok.store.Stone;

/**
 * Validates whether a move request can proceed.
 */
public final class MoveValidatingState implements GameSessionState {
    @Override
    public GameSessionStateType type() {
        return GameSessionStateType.MOVE_VALIDATING;
    }

    @Override
    public GameSessionStateStep onEnter(GameSessionStateContext context) {
        TurnCycleContext cycle = context.activeTurnCycle();
        if (cycle == null) {
            return GameSessionStateStep.transition(GameSessionStateType.TURN_WAITING);
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
            return GameSessionStateStep.transition(GameSessionStateType.TURN_WAITING);
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
            return GameSessionStateStep.transition(GameSessionStateType.TURN_WAITING);
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
            return GameSessionStateStep.transition(GameSessionStateType.TURN_WAITING);
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
            return GameSessionStateStep.transition(GameSessionStateType.TURN_WAITING);
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
            return GameSessionStateStep.transition(GameSessionStateType.TURN_WAITING);
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
            return GameSessionStateStep.transition(GameSessionStateType.TURN_WAITING);
        }

        Stone stone = Stone.fromPlayerOrder(playerIndex);
        cycle.stone(stone);
        return GameSessionStateStep.transition(GameSessionStateType.MOVE_APPLYING);
    }

    private void invalidate(GameSessionStateContext context, MoveResult result) {
        context.pendingMoveResult(result);
        context.clearTurnCycle();
    }
}
