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
        TurnCursor cursor = captureTurnCursor(runtime);
        HiddenPlacementCoordinator.HiddenPlacement placement =
            new HiddenPlacementCoordinator.HiddenPlacement(
                frame.userId(),
                x,
                y,
                index,
                stone,
                frame.requestedAtMillis(),
                frame.stonePlaceRequestId(),
                cursor.turn(),
                cursor.position()
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
        TurnCursor cursor = captureTurnCursor(runtime);
        List<HiddenPlacementCoordinator.HiddenPlacement> carry = new ArrayList<>();
        boolean revealed = false;
        for (HiddenPlacementCoordinator.HiddenPlacement placement : pending) {
            if (shouldRevealPlacement(placement, cursor)) {
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

    private boolean shouldRevealPlacement(HiddenPlacementCoordinator.HiddenPlacement placement,
                                          TurnCursor cursor) {
        int originTurn = placement.originTurn();
        if (cursor.turn() >= 0 && originTurn >= 0 && cursor.turn() - originTurn >= 2) {
            return true;
        }
        int originPosition = placement.originPosition();
        if (cursor.position() <= 0 || originPosition <= 0) {
            return true;
        }
        return cursor.position() == originPosition;
    }

    private TurnCursor captureTurnCursor(RuleRuntimeContext runtime) {
        TurnSnapshot snapshot = runtime.turnSnapshot();
        if (snapshot != null) {
            return new TurnCursor(
                Math.max(0, snapshot.turnNumber()),
                Math.max(0, snapshot.positionInRound())
            );
        }
        GameSessionStateContext context = runtime.stateContext();
        if (context != null) {
            GameSessionTurnAccess turns = context.turns();
            if (turns != null) {
                int turnNumber = Math.max(0, turns.actionNumber());
                TurnCounters counters = turns.counters();
                int position = counters != null ? Math.max(0, counters.positionInRound()) : 0;
                return new TurnCursor(turnNumber, position);
            }
        }
        return new TurnCursor(0, 0);
    }

    private record TurnCursor(int turn, int position) {
    }
}
