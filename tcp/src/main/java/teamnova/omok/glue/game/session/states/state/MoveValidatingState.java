package teamnova.omok.glue.game.session.states.state;

import java.util.Objects;

import teamnova.omok.glue.game.session.interfaces.GameBoardService;
import teamnova.omok.glue.game.session.interfaces.GameTurnService;
import teamnova.omok.glue.game.session.log.TurnStateLogger;
import teamnova.omok.glue.game.session.model.Stone;
import teamnova.omok.glue.game.session.model.dto.TurnSnapshot;
import teamnova.omok.glue.game.session.model.result.MoveStatus;
import teamnova.omok.glue.game.session.model.runtime.TurnPersonalFrame;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContext;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContextService;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateType;
import teamnova.omok.glue.rule.rules.ProtectiveZoneRule;
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
        TurnPersonalFrame frame = contextService.turn().currentPersonalTurn(context);
        if (frame == null || !frame.hasActiveMove()) {
            return StateStep.transition(GameSessionStateType.TURN_WAITING.toStateName());
        }
        String userId = frame.userId();
        int x = frame.x();
        int y = frame.y();

        int playerIndex = context.participants().playerIndexOf(userId);
        if (playerIndex < 0) {
            invalidate(context, frame, MoveStatus.INVALID_PLAYER, null);
            return StateStep.transition(GameSessionStateType.TURN_WAITING.toStateName());
        }
        if (!context.lifecycle().isGameStarted()) {
            invalidate(context, frame, MoveStatus.GAME_NOT_STARTED, null);
            return StateStep.transition(GameSessionStateType.TURN_WAITING.toStateName());
        }

        TurnSnapshot currentSnapshot =
            turnService.snapshot(context.turns());
        frame.currentSnapshot(currentSnapshot);

        if (context.outcomes().isGameFinished()) {
            invalidate(context, frame, MoveStatus.GAME_FINISHED, currentSnapshot);
            return StateStep.transition(GameSessionStateType.TURN_WAITING.toStateName());
        }

        if (!boardService.isWithinBounds(context.board(), x, y)) {
            invalidate(context, frame, MoveStatus.OUT_OF_BOUNDS, currentSnapshot);
            return StateStep.transition(GameSessionStateType.TURN_WAITING.toStateName());
        }

        Integer currentIndex = turnService.currentPlayerIndex(context.turns());
        if (currentIndex == null || currentIndex != playerIndex) {
            invalidate(context, frame, MoveStatus.OUT_OF_TURN, currentSnapshot);
            return StateStep.transition(GameSessionStateType.TURN_WAITING.toStateName());
        }

        if (!boardService.isEmpty(context.board(), x, y)) {
            invalidate(context, frame, MoveStatus.CELL_OCCUPIED, currentSnapshot);
            return StateStep.transition(GameSessionStateType.TURN_WAITING.toStateName());
        }

        if (violatesProtectiveZone(context, x, y)) {
            invalidate(context, frame, MoveStatus.RESTRICTED_ZONE, currentSnapshot);
            return StateStep.transition(GameSessionStateType.TURN_WAITING.toStateName());
        }

        Stone stone = Stone.fromPlayerOrder(playerIndex);
        frame.stone(stone);
        return StateStep.transition(GameSessionStateType.MOVE_APPLYING.toStateName());
    }

    private void invalidate(GameSessionStateContext context,
                            TurnPersonalFrame frame,
                            MoveStatus status,
                            TurnSnapshot snapshot) {
        if (snapshot != null) {
            frame.currentSnapshot(snapshot);
        }
        TurnStateLogger.event(context, GameSessionStateType.MOVE_VALIDATING, "MoveRejected",
            "status=" + status,
            String.format("user=%s x=%d y=%d", frame.userId(), frame.x(), frame.y()));
        contextService.turn().finalizeMoveOutcome(context, status);
        contextService.turn().clearTurnCycle(context);
    }

    private boolean violatesProtectiveZone(GameSessionStateContext context, int x, int y) {
        if (context == null) {
            return false;
        }
        Object state = context.rules().getRuleData(ProtectiveZoneRule.STORAGE_KEY);
        return ProtectiveZoneRule.isRestricted(state, x, y);
    }
}
