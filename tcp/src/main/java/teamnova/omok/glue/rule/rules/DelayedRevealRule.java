package teamnova.omok.glue.rule.rules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import teamnova.omok.glue.game.session.interfaces.GameSessionMessenger;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionBoardAccess;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionRuleAccess;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionTurnAccess;
import teamnova.omok.glue.game.session.model.Stone;
import teamnova.omok.glue.game.session.model.dto.TurnSnapshot;
import teamnova.omok.glue.game.session.model.runtime.TurnPersonalFrame;
import teamnova.omok.glue.game.session.model.vo.TurnCounters;
import teamnova.omok.glue.game.session.services.HiddenPlacementCoordinator;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContext;
import teamnova.omok.glue.rule.api.BoardTransformRule;
import teamnova.omok.glue.rule.api.HiddenPlacementRule;
import teamnova.omok.glue.rule.api.Rule;
import teamnova.omok.glue.rule.api.RuleId;
import teamnova.omok.glue.rule.api.RuleMetadata;
import teamnova.omok.glue.rule.api.RuleTriggerKind;
import teamnova.omok.glue.rule.runtime.RuleRuntimeContext;

/**
 * 시간차 소환: 돌을 숨겼다가 한 턴 뒤에 공개하며, 충돌 시 최근 돌로 대체한다.
 * 호출 시점: 턴 시작 시 및 돌 배치 후.
 */
public final class DelayedRevealRule implements Rule, HiddenPlacementRule, BoardTransformRule {
    private static final RuleMetadata METADATA = new RuleMetadata(
        RuleId.DELAYED_REVEAL,
        2_300
    );
    @Override
    public RuleMetadata getMetadata() {
        return METADATA;
    }

    @Override
    public void invoke(GameSessionRuleAccess access, RuleRuntimeContext runtime) {
        // No-op: hidden placement lifecycle is coordinated via the capability hooks.
    }

    @Override
    public boolean queueHiddenPlacement(GameSessionRuleAccess access, RuleRuntimeContext runtime) {
        if (runtime == null || runtime.stateContext() == null || runtime.contextService() == null) {
            return false;
        }
        GameSessionStateContext context = runtime.stateContext();
        TurnPersonalFrame frame = runtime.contextService().turn().currentPersonalTurn(context);
        if (frame == null) {
            return false;
        }
        GameSessionBoardAccess board = context.board();
        if (board == null) {
            return false;
        }
        int x = frame.x();
        int y = frame.y();
        if (!board.isWithinBounds(x, y)) {
            return false;
        }
        int width = board.width();
        int index = y * width + x;
        Stone stone = frame.stone();
        if (stone == null || stone == Stone.EMPTY) {
            return false;
        }
        HiddenPlacementCoordinator coordinator = runtime.services().hiddenPlacementCoordinator();
        HiddenPlacementCoordinator.HiddenPlacement placement =
            new HiddenPlacementCoordinator.HiddenPlacement(
                frame.userId(),
                x,
                y,
                index,
                stone,
                frame.requestedAtMillis(),
                frame.stonePlaceRequestId(),
                resolvePlacementTurn(runtime, context),
                resolvePlacementPosition(runtime, context)
            );
        coordinator.queue(access, placement);
        return true;
    }

    @Override
    public void revealHiddenPlacements(GameSessionRuleAccess access, RuleRuntimeContext runtime) {
        if (runtime == null || runtime.triggerKind() != RuleTriggerKind.TURN_START) {
            return;
        }
        HiddenPlacementCoordinator coordinator = runtime.services().hiddenPlacementCoordinator();
        List<HiddenPlacementCoordinator.HiddenPlacement> pending = coordinator.drain(access);
        if (pending.isEmpty()) {
            return;
        }
        TurnSnapshot snapshot = runtime.turnSnapshot();
        int currentTurn = snapshot != null ? snapshot.turnNumber() : resolveCurrentTurn(runtime.stateContext());
        int currentPosition = snapshot != null ? snapshot.positionInRound() : resolveCurrentPosition(runtime.stateContext());
        List<HiddenPlacementCoordinator.HiddenPlacement> carry = new ArrayList<>();
        boolean revealed = false;
        for (HiddenPlacementCoordinator.HiddenPlacement placement : pending) {
            if (shouldRevealPlacement(placement, currentTurn, currentPosition)) {
                revealed = true;
            } else {
                carry.add(placement);
            }
        }
        for (HiddenPlacementCoordinator.HiddenPlacement placement : carry) {
            coordinator.queue(access, placement);
        }
        if (revealed) {
            GameSessionMessenger messenger = runtime.services().messenger();
            if (messenger != null) {
                messenger.broadcastBoardSnapshot(runtime.stateContext().session());
            }
        }
    }

    @Override
    public byte[] transformBoard(GameSessionRuleAccess access, byte[] snapshot) {
        if (access == null || snapshot == null || snapshot.length == 0) {
            return snapshot;
        }
        HiddenPlacementCoordinator coordinator = new HiddenPlacementCoordinator();
        List<HiddenPlacementCoordinator.HiddenPlacement> pending = coordinator.snapshot(access);
        if (pending.isEmpty()) {
            return snapshot;
        }
        byte[] copy = Arrays.copyOf(snapshot, snapshot.length);
        for (HiddenPlacementCoordinator.HiddenPlacement placement : pending) {
            int index = placement.index();
            if (index >= 0 && index < copy.length) {
                copy[index] = Stone.EMPTY.code();
            }
        }
        return copy;
    }

    private int resolvePlacementPosition(RuleRuntimeContext runtime, GameSessionStateContext context) {
        TurnSnapshot snapshot = runtime.turnSnapshot();
        if (snapshot != null) {
            return Math.max(0, snapshot.positionInRound());
        }
        if (context != null) {
            GameSessionTurnAccess turns = context.turns();
            if (turns != null) {
                TurnCounters counters = turns.counters();
                if (counters != null) {
                    return Math.max(0, counters.positionInRound());
                }
            }
        }
        return 0;
    }

    private int resolveCurrentPosition(GameSessionStateContext context) {
        if (context == null) {
            return -1;
        }
        GameSessionTurnAccess turns = context.turns();
        if (turns == null) {
            return -1;
        }
        TurnCounters counters = turns.counters();
        return counters != null ? counters.positionInRound() : -1;
    }

    private int resolvePlacementTurn(RuleRuntimeContext runtime, GameSessionStateContext context) {
        TurnSnapshot snapshot = runtime.turnSnapshot();
        if (snapshot != null) {
            return Math.max(0, snapshot.turnNumber());
        }
        if (context != null) {
            GameSessionTurnAccess turns = context.turns();
            if (turns != null) {
                return Math.max(0, turns.actionNumber());
            }
        }
        return 0;
    }

    private int resolveCurrentTurn(GameSessionStateContext context) {
        if (context == null) {
            return -1;
        }
        GameSessionTurnAccess turns = context.turns();
        if (turns == null) {
            return -1;
        }
        return Math.max(0, turns.actionNumber());
    }

    private boolean shouldRevealPlacement(HiddenPlacementCoordinator.HiddenPlacement placement,
                                          int currentTurn,
                                          int currentPosition) {
        int originTurn = placement.originTurn();
        if (currentTurn >= 0 && originTurn >= 0 && currentTurn - originTurn >= 2) {
            return true;
        }
        int originPosition = placement.originPosition();
        if (currentPosition <= 0 || originPosition <= 0) {
            return true;
        }
        return currentPosition == originPosition;
    }
}
