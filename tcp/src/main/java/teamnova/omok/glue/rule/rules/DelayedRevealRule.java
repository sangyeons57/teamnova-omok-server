package teamnova.omok.glue.rule.rules;

import java.util.ArrayList;
import java.util.List;

import teamnova.omok.glue.game.session.interfaces.GameBoardService;
import teamnova.omok.glue.game.session.interfaces.GameSessionMessenger;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionBoardAccess;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionRuleAccess;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionTurnAccess;
import teamnova.omok.glue.game.session.model.Stone;
import teamnova.omok.glue.game.session.model.dto.TurnSnapshot;
import teamnova.omok.glue.game.session.model.runtime.TurnPersonalFrame;
import teamnova.omok.glue.game.session.model.vo.StonePlacementMetadata;
import teamnova.omok.glue.game.session.model.vo.TurnCounters;
import teamnova.omok.glue.game.session.services.HiddenPlacementCoordinator;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContext;
import teamnova.omok.glue.rule.api.Rule;
import teamnova.omok.glue.rule.api.RuleId;
import teamnova.omok.glue.rule.api.RuleMetadata;
import teamnova.omok.glue.rule.api.RuleTriggerKind;
import teamnova.omok.glue.rule.runtime.RuleRuntimeContext;

/**
 * 시간차 소환: 돌을 숨겼다가 한 턴 뒤에 공개하며, 충돌 시 최근 돌로 대체한다.
 * 호출 시점: 턴 시작 시 및 돌 배치 후.
 * 통과(2025.10.29)
 */
public final class DelayedRevealRule implements Rule {
    private static final RuleMetadata METADATA = new RuleMetadata(
        RuleId.DELAYED_REVEAL,
        2_300
    );
    public static final String SKIP_PLACEMENT_KEY = "rules.delayedReveal.skipPlacement";
    @Override
    public RuleMetadata getMetadata() {
        return METADATA;
    }

    @Override
    public void invoke(GameSessionRuleAccess access, RuleRuntimeContext runtime) {
        if (access == null || runtime == null) {
            return;
        }
        RuleTriggerKind kind = runtime.triggerKind();
        if (kind == RuleTriggerKind.PRE_PLACEMENT) {
            handlePrePlacement(access, runtime);
        } else if (kind == RuleTriggerKind.TURN_START) {
            handleTurnStart(access, runtime);
        }
    }

    private void handlePrePlacement(GameSessionRuleAccess access, RuleRuntimeContext runtime) {
        if (runtime.stateContext() == null || runtime.contextService() == null) {
            return;
        }
        GameSessionStateContext context = runtime.stateContext();
        TurnPersonalFrame frame = runtime.contextService().turn().currentPersonalTurn(context);
        if (frame == null) {
            return;
        }
        GameSessionBoardAccess board = context.board();
        if (board == null) {
            return;
        }
        int x = frame.x();
        int y = frame.y();
        if (!board.isWithinBounds(x, y)) {
            return;
        }
        int width = board.width();
        int index = y * width + x;
        Stone stone = frame.stone();
        if (stone == null || stone == Stone.EMPTY) {
            return;
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
        access.putRuleData(SKIP_PLACEMENT_KEY, placement);
    }

    private void handleTurnStart(GameSessionRuleAccess access, RuleRuntimeContext runtime) {
        HiddenPlacementCoordinator coordinator = runtime.services().hiddenPlacementCoordinator();
        List<HiddenPlacementCoordinator.HiddenPlacement> pending = coordinator.drain(access);
        if (pending.isEmpty()) {
            return;
        }
        TurnCursor cursor = captureTurnCursor(runtime);
        List<HiddenPlacementCoordinator.HiddenPlacement> carry = new ArrayList<>();
        List<HiddenPlacementCoordinator.HiddenPlacement> toReveal = new ArrayList<>();
        boolean revealed = false;
        for (HiddenPlacementCoordinator.HiddenPlacement placement : pending) {
            if (shouldRevealPlacement(placement, cursor)) {
                revealed = true;
                toReveal.add(placement);
            } else {
                carry.add(placement);
            }
        }
        for (HiddenPlacementCoordinator.HiddenPlacement placement : carry) {
            coordinator.queue(access, placement);
        }
        if (revealed) {
            applyRevealedPlacements(runtime, toReveal, cursor);
        }
        GameSessionMessenger messenger = runtime.services().messenger();
        if (messenger != null) {
            messenger.broadcastBoardSnapshot(runtime.stateContext().session());
        }
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

    private void applyRevealedPlacements(RuleRuntimeContext runtime,
                                         List<HiddenPlacementCoordinator.HiddenPlacement> placements,
                                         TurnCursor cursor) {
        if (placements.isEmpty()) {
            return;
        }
        GameSessionStateContext context = runtime.stateContext();
        if (context == null) {
            return;
        }
        GameSessionBoardAccess board = context.board();
        if (board == null) {
            return;
        }
        GameBoardService boardService = runtime.services().boardService();
        TurnSnapshot snapshot = runtime.turnSnapshot();
        for (HiddenPlacementCoordinator.HiddenPlacement placement : placements) {
            StonePlacementMetadata metadata = resolveMetadataForReveal(context, snapshot, placement, cursor.turn());
            boardService.setStone(board, placement.x(), placement.y(), placement.stone(), metadata);
        }
    }

    private StonePlacementMetadata resolveMetadataForReveal(GameSessionStateContext context,
                                                            TurnSnapshot snapshot,
                                                            HiddenPlacementCoordinator.HiddenPlacement placement,
                                                            int currentTurn) {
        int playerIndex = context.participants().playerIndexOf(placement.userId());
        if (playerIndex >= 0 && snapshot != null) {
            return StonePlacementMetadata.forRule(snapshot, playerIndex, placement.userId());
        }
        if (playerIndex >= 0) {
            return new StonePlacementMetadata(Math.max(0, currentTurn), playerIndex, placement.userId(), StonePlacementMetadata.Source.RULE);
        }
        return StonePlacementMetadata.systemGenerated();
    }

    private record TurnCursor(int turn, int position) {
    }
}
