package teamnova.omok.glue.rule.rules;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionRuleAccess;
import teamnova.omok.glue.game.session.model.PlayerResult;
import teamnova.omok.glue.rule.api.RuleTriggerKind;
import teamnova.omok.glue.rule.api.OutcomeResolution;
import teamnova.omok.glue.rule.api.OutcomeRule;
import teamnova.omok.glue.rule.api.Rule;
import teamnova.omok.glue.rule.api.RuleId;
import teamnova.omok.glue.rule.api.RuleMetadata;
import teamnova.omok.glue.rule.runtime.RuleRuntimeContext;

/**
 * 럭키 7: 7의 배수 턴에 승리 조건을 달성한 플레이어를 패배 처리한다.
 * 호출 시점: 게임 종료 시.
 */
public final class LuckySevenRule implements Rule, OutcomeRule {
    private static final RuleMetadata METADATA = new RuleMetadata(
        RuleId.LUCKY_SEVEN,
        2_100
    );

    @Override
    public RuleMetadata getMetadata() {
        return METADATA;
    }

    @Override
    public void invoke(GameSessionRuleAccess access, RuleRuntimeContext runtime) {
        // Primary behaviour executed via resolveOutcome.
    }

    @Override
    public Optional<OutcomeResolution> resolveOutcome(GameSessionRuleAccess access,
                                                      RuleRuntimeContext runtime,
                                                      OutcomeResolution currentOutcome) {
        System.out.println("LuckySevenRule: resolveOutcome");
        System.out.println("access:" + access + ", runtime:" + runtime + ", currentOutcome:" + currentOutcome
        + ", turnNumber:" + runtime.turnSnapshot().turnNumber() + ", triggerKind:" + runtime.triggerKind() );
        if (access == null || runtime == null || runtime.turnSnapshot() == null
            || runtime.triggerKind() != RuleTriggerKind.OUTCOME_EVALUATION
            || currentOutcome == null) {
            return Optional.empty();
        }
        System.out.println("LuckySevenRule: resolveOutcome: " + currentOutcome.assignments());
        int turnNumber = runtime.turnSnapshot().turnNumber();
        System.out.println("turnNumber:" + turnNumber);
        if (turnNumber <= 0 || turnNumber % 7 != 0) {
            return Optional.empty();
        }
        Map<String, PlayerResult> assignments = new LinkedHashMap<>();
        currentOutcome.assignments().forEach((userId, result) -> {
            System.out.println("LuckySevenRule: " + userId + " : " + result);
            if (result == PlayerResult.WIN) {
                System.out.println("LuckySevenRule: " + userId + " : " + result + "[WIN]");
                assignments.put(userId, PlayerResult.LOSS);
            }
        });
        if (assignments.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(OutcomeResolution.of(assignments, true));
    }
}
