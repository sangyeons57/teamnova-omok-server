package teamnova.omok.glue.rule.rules;

import java.util.List;

import teamnova.omok.glue.game.session.interfaces.GameSessionMessenger;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionRuleAccess;
import teamnova.omok.glue.game.session.model.PlayerResult;
import teamnova.omok.glue.game.session.services.TurnBudgetManager;
import teamnova.omok.glue.rule.api.Rule;
import teamnova.omok.glue.rule.api.RuleId;
import teamnova.omok.glue.rule.api.RuleMetadata;
import teamnova.omok.glue.rule.api.RuleTriggerKind;
import teamnova.omok.glue.rule.api.TurnBudgetRule;
import teamnova.omok.glue.rule.runtime.RuleDataKeys;
import teamnova.omok.glue.rule.runtime.RuleRuntimeContext;

/**
 * 스피드 게임 2: 총 100초 타이머를 소진하면 해당 플레이어를 패배 처리한다.
 * 호출 시점: 게임 진행 중.
 */
public final class SpeedGame2Rule implements Rule, TurnBudgetRule {
    private static final RuleMetadata METADATA = new RuleMetadata(
        RuleId.SPEED_GAME_2,
        900
    );
    private static final long TOTAL_BUDGET_MILLIS = 100_000L;

    @Override
    public RuleMetadata getMetadata() {
        return METADATA;
    }

    @Override
    public void invoke(GameSessionRuleAccess context, RuleRuntimeContext runtime) {
        // Primary behaviour executed via updateTurnBudget.
    }

    @Override
    public boolean updateTurnBudget(GameSessionRuleAccess access, RuleRuntimeContext runtime) {
        if (access == null || runtime == null || runtime.stateContext() == null) {
            return false;
        }
        TurnBudgetManager manager = runtime.services().turnBudgetManager();
        if (manager == null) {
            return false;
        }
        switch (runtime.triggerKind()) {
            case GAME_START -> {
                List<String> participants = runtime.stateContext().participants().getUserIds();
                for (String userId : participants) {
                    manager.update(access, userId, TOTAL_BUDGET_MILLIS);
                }
                return true;
            }
            case TURN_START -> {
                if (runtime.turnSnapshot() != null) {
                    access.putRuleData(RuleDataKeys.SPEED_GAME_LAST_PLAYER, runtime.turnSnapshot().currentPlayerId());
                    access.putRuleData(RuleDataKeys.SPEED_GAME_LAST_TURN_START, runtime.turnSnapshot().turnStartAt());
                }
                return false;
            }
            case TURN_ADVANCE -> {
                String lastPlayer = (String) access.getRuleData(RuleDataKeys.SPEED_GAME_LAST_PLAYER);
                Long lastStart = (Long) access.getRuleData(RuleDataKeys.SPEED_GAME_LAST_TURN_START);
                if (lastPlayer != null && lastStart != null && runtime.turnSnapshot() != null) {
                    long nextStart = runtime.turnSnapshot().turnStartAt();
                    long elapsed = Math.max(0L, nextStart - lastStart);
                    long remaining = manager.decrement(access, lastPlayer, elapsed);
                    if (remaining <= 0L) {
                        resolveTimeoutLoss(runtime, lastPlayer);
                    }
                }
                if (runtime.turnSnapshot() != null) {
                    access.putRuleData(RuleDataKeys.SPEED_GAME_LAST_PLAYER, runtime.turnSnapshot().currentPlayerId());
                    access.putRuleData(RuleDataKeys.SPEED_GAME_LAST_TURN_START, runtime.turnSnapshot().turnStartAt());
                }
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    private void resolveTimeoutLoss(RuleRuntimeContext runtime, String playerId) {
        var context = runtime.stateContext();
        context.outcomes().updateOutcome(playerId, PlayerResult.LOSS);
        for (String opponent : context.participants().getUserIds()) {
            if (!opponent.equals(playerId)) {
                context.outcomes().updateOutcome(opponent, PlayerResult.WIN);
            }
        }
        GameSessionMessenger messenger = runtime.services().messenger();
        if (messenger != null) {
            messenger.broadcastGameCompleted(context.session());
        }
    }
}
