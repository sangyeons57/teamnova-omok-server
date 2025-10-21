package teamnova.omok.glue.rule.rules;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionRuleAccess;
import teamnova.omok.glue.rule.Rule;
import teamnova.omok.glue.rule.RuleId;
import teamnova.omok.glue.rule.RuleMetadata;
import teamnova.omok.glue.rule.RuleRuntimeContext;

/**
 * 방해 금지: 판 위의 모든 방해돌을 제거한다.
 * 호출 시점: 게임 진행 중.
 */
public final class BlockerBanRule implements Rule {
    private static final RuleMetadata METADATA = new RuleMetadata(
        RuleId.BLOCKER_BAN,
        1_200
    );

    @Override
    public RuleMetadata getMetadata() {
        return METADATA;
    }

    @Override
    public void invoke(GameSessionRuleAccess access, RuleRuntimeContext runtime) {
        // Not implemented: purging blockers each trigger would conflict with rules
        // that rely on them (infection, random blockers) and requires clarifying
        // when the purge runs. Until rule priority is defined, this rule remains idle.
    }
}
