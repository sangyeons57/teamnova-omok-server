package teamnova.omok.glue.game.session.states.state;

import java.util.Objects;

import teamnova.omok.glue.game.session.log.GameSessionLogger;
import teamnova.omok.glue.game.session.model.Stone;
import teamnova.omok.glue.game.session.model.dto.GameSessionServices;
import teamnova.omok.glue.game.session.model.dto.TurnSnapshot;
import teamnova.omok.glue.game.session.model.result.MoveStatus;
import teamnova.omok.glue.game.session.model.runtime.TurnPersonalFrame;
import teamnova.omok.glue.game.session.services.RuleService;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContext;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContextService;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateType;
import teamnova.omok.glue.rule.api.RuleTriggerKind;
import teamnova.omok.glue.rule.rules.ProtectiveZoneRule;
import teamnova.omok.glue.rule.runtime.RuleRuntimeContext;
import teamnova.omok.modules.state_machine.interfaces.BaseState;
import teamnova.omok.modules.state_machine.interfaces.StateContext;
import teamnova.omok.modules.state_machine.models.StateName;
import teamnova.omok.modules.state_machine.models.StateStep;

/**
 * Validates whether a move request can proceed.
 */
public final class MoveValidatingState implements BaseState {
    private final GameSessionStateContextService contextService;
    private final GameSessionServices services;

    public MoveValidatingState(GameSessionStateContextService contextService,
                               GameSessionServices services) {
        this.contextService = Objects.requireNonNull(contextService, "contextService");
        this.services = Objects.requireNonNull(services, "services");
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
            services.turnService().snapshot(context.turns());
        frame.currentSnapshot(currentSnapshot);

        if (context.outcomes().isGameFinished()) {
            invalidate(context, frame, MoveStatus.GAME_FINISHED, currentSnapshot);
            return StateStep.transition(GameSessionStateType.TURN_WAITING.toStateName());
        }

        if (!services.boardService().isWithinBounds(context.board(), x, y)) {
            invalidate(context, frame, MoveStatus.OUT_OF_BOUNDS, currentSnapshot);
            return StateStep.transition(GameSessionStateType.TURN_WAITING.toStateName());
        }

        if (currentSnapshot == null || !userId.equals(currentSnapshot.currentPlayerId())) {
            invalidate(context, frame, MoveStatus.OUT_OF_TURN, currentSnapshot);
            return StateStep.transition(GameSessionStateType.TURN_WAITING.toStateName());
        }
        // 참고: 순환 순서를 인덱스 배열(예: [2,1,3,4])로 저장해 비교하는 방식도 가능하지만,
        // 현재는 TurnOrder의 userId와 비교해 검증하고, 돌 저장 시 고정 참가자 인덱스를 사용한다.

        if (!services.boardService().isEmpty(context.board(), x, y)) {
            invalidate(context, frame, MoveStatus.CELL_OCCUPIED, currentSnapshot);
            return StateStep.transition(GameSessionStateType.TURN_WAITING.toStateName());
        }

        if (violatesProtectiveZone(context, currentSnapshot, x, y)) {
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
        } else {
            // Ensure snapshot is available for responders/encoders that expect it
            TurnSnapshot current = services.turnService().snapshot(context.turns());
            if (current != null) {
                frame.currentSnapshot(current);
            }
        }
        GameSessionLogger.event(context, GameSessionStateType.MOVE_VALIDATING, "MoveRejected",
            "status=" + status,
            String.format("user=%s x=%d y=%d", frame.userId(), frame.x(), frame.y()));
        contextService.turn().finalizeMoveOutcome(context, status);
        contextService.turn().clearTurnCycle(context);
        // Send immediate error ACK so clients can react without waiting for TURN_PERSONAL_END
        services.messenger().respondMove(frame.userId(), frame.stonePlaceRequestId(), context.session(), frame);
    }

    private boolean violatesProtectiveZone(GameSessionStateContext context,
                                           TurnSnapshot snapshot,
                                           int x,
                                           int y) {
        if (context == null) {
            return false;
        }
        fireProtectiveZoneRule(context, snapshot);
        if (ProtectiveZoneRule.consumeValidationResult(context.rules())) {
            return true;
        }
        Object state = context.rules().getRuleData(ProtectiveZoneRule.STORAGE_KEY);
        return ProtectiveZoneRule.isRestricted(state, x, y);
    }

    private void fireProtectiveZoneRule(GameSessionStateContext context, TurnSnapshot snapshot) {
        TurnSnapshot effectiveSnapshot = snapshot;
        if (effectiveSnapshot == null) {
            effectiveSnapshot = services.turnService().snapshot(context.turns());
        }
        if (effectiveSnapshot == null) {
            return;
        }
        RuleService ruleService = RuleService.getInstance();
        RuleRuntimeContext runtime = new RuleRuntimeContext(
            services,
            contextService,
            context,
            effectiveSnapshot,
            RuleTriggerKind.MOVE_VALIDATION
        );
        ruleService.activateRules(context.rules(), runtime);
    }
}
