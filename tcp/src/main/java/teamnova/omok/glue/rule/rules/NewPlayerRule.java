package teamnova.omok.glue.rule.rules;

import java.util.Map;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionRuleAccess;
import teamnova.omok.glue.game.session.model.PlayerResult;
import teamnova.omok.glue.rule.api.ParticipantOutcomeRule;
import teamnova.omok.glue.rule.api.Rule;
import teamnova.omok.glue.rule.api.RuleId;
import teamnova.omok.glue.rule.api.RuleMetadata;
import teamnova.omok.glue.rule.runtime.RuleDataKeys;
import teamnova.omok.glue.rule.runtime.RuleRuntimeContext;

/**
 * 새로운 플레이어: 비플레이어 돌을 가상의 플레이어 소유로 간주하여 승패를 판정한다.
 * 호출 시점: 돌 배치 후.
 */
public final class NewPlayerRule implements Rule, ParticipantOutcomeRule {
    private static final RuleMetadata METADATA = new RuleMetadata(
        RuleId.NEW_PLAYER,
        2_200
    );

    @Override
    public RuleMetadata getMetadata() {
        return METADATA;
    }

    @Override
    public void invoke(GameSessionRuleAccess access, RuleRuntimeContext runtime) {
        // Primary behaviour executed via registerParticipantOutcomes.
    }

    @Override
    public Map<String, PlayerResult> registerParticipantOutcomes(GameSessionRuleAccess access,
                                                                 RuleRuntimeContext runtime) {
        if (access == null || runtime == null || runtime.stateContext() == null) {
            return Map.of();
        }
        String existingId = (String) access.getRuleData(RuleDataKeys.NEW_PLAYER_ID);
        if (existingId == null) {
            existingId = "virtual:" + runtime.stateContext().session().sessionId().asUuid();
            access.putRuleData(RuleDataKeys.NEW_PLAYER_ID, existingId);
        }
        return Map.of(existingId, PlayerResult.PENDING);
    }
}
