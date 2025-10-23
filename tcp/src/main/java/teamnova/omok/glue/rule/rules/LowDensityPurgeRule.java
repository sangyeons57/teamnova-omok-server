package teamnova.omok.glue.rule.rules;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionRuleAccess;
import teamnova.omok.glue.rule.api.Rule;
import teamnova.omok.glue.rule.api.RuleId;
import teamnova.omok.glue.rule.api.RuleMetadata;
import teamnova.omok.glue.rule.runtime.RuleRuntimeContext;

/**
 * 뭉쳐야 산다: 5턴마다 주변에 돌이 가장 적은 돌들을 제거한다.
 * 호출 시점: 전체 턴 종료 시.
 */
public final class LowDensityPurgeRule implements Rule {
    private static final RuleMetadata METADATA = new RuleMetadata(
        RuleId.LOW_DENSITY_PURGE,
        1_700
    );

    @Override
    public RuleMetadata getMetadata() {
        return METADATA;
    }

    @Override
    public void invoke(GameSessionRuleAccess access, RuleRuntimeContext runtime) {
        // Not implemented: computing "저밀도" neighbourhoods needs a board-wide density scan and
        // tie-breaking rules (especially under simultaneous removals). Those algorithms are not yet
        // defined, so the rule remains inactive.
    }
}
