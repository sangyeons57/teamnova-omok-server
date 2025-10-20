package teamnova.omok.glue.game.session.states.state;

import java.util.Objects;

import teamnova.omok.glue.game.session.interfaces.GameBoardService;
import teamnova.omok.glue.game.session.interfaces.GameTurnService;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionAccess;
import teamnova.omok.glue.game.session.model.GameSession;
import teamnova.omok.glue.game.session.model.Stone;
import teamnova.omok.glue.game.session.model.result.MoveResult;
import teamnova.omok.glue.game.session.model.result.MoveStatus;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContext;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContextService;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateType;
import teamnova.omok.glue.game.session.states.manage.TurnCycleContext;
import teamnova.omok.modules.state_machine.interfaces.BaseState;
import teamnova.omok.modules.state_machine.interfaces.StateContext;
import teamnova.omok.modules.state_machine.models.StateName;
import teamnova.omok.modules.state_machine.models.StateStep;

/**
 * Validates whether a move request can proceed.
 */
public final class MoveValidatingState implements BaseState {
    private final GameBoardService boardService;
    private final GameTurnService turnService;
    private final GameSessionStateContextService contextService;

    public MoveValidatingState(GameSessionStateContextService contextService,
                               GameBoardService boardService,
                               GameTurnService turnService) {
        this.contextService = Objects.requireNonNull(contextService, "contextService");
        this.boardService = Objects.requireNonNull(boardService, "boardService");
        this.turnService = Objects.requireNonNull(turnService, "turnService");
    }
    @Override
    public StateName name() {
        return GameSessionStateType.MOVE_VALIDATING.toStateName();
    }

    @Override
    public <I extends StateContext> StateStep onEnter(I context) {
        return onEnterInternal((GameSessionStateContext) context);
    }

    private StateStep onEnterInternal(GameSessionStateContext context) {
        TurnCycleContext cycle = contextService.turn().activeTurnCycle(context);
        if (cycle == null) {
            return StateStep.transition(GameSessionStateType.TURN_WAITING.toStateName());
        }
        GameSessionAccess session = cycle.session();
        String userId = cycle.userId();
        int x = cycle.x();
        int y = cycle.y();

        int playerIndex = context.participants().playerIndexOf(userId);
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
        if (!context.lifecycle().isGameStarted()) {
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

        GameTurnService.TurnSnapshot currentSnapshot =
            turnService.snapshot(context.turns());
        cycle.snapshots().current(currentSnapshot);

        if (context.outcomes().isGameFinished()) {
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

        if (!boardService.isWithinBounds(context.board(), x, y)) {
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

        Integer currentIndex = turnService.currentPlayerIndex(context.turns());
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

        if (!boardService.isEmpty(context.board(), x, y)) {
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
        contextService.turn().queueMoveResult(context, result);
        contextService.turn().clearTurnCycle(context);
    }
}
