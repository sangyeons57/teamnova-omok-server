package teamnova.omok.glue.rule.rules;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionRuleAccess;
import teamnova.omok.glue.rule.Rule;
import teamnova.omok.glue.rule.RuleId;
import teamnova.omok.glue.rule.RuleMetadata;
import teamnova.omok.glue.rule.RuleRuntimeContext;

/**
 * 거울: 판을 대칭으로 나누어 한쪽 행동이 반대편에도 복제되도록 한다.
 * 호출 시점: 돌 배치 후.
 */
public final class MirrorBoardRule implements Rule {
    private static final RuleMetadata METADATA = new RuleMetadata(
        RuleId.MIRROR_BOARD,
        1_300
    );

    @Override
    public RuleMetadata getMetadata() {
        return METADATA;
    }

    @Override
    public void invoke(GameSessionRuleAccess access, RuleRuntimeContext runtime) {
        // Not implemented: mirroring requires deterministic overwrite rules for occupied
        // target cells and coordination with rule-triggered placements (e.g. blockers).
        // Without explicit conflict resolution from design, the behaviour would be
        // ambiguous, so this rule is currently a no-op.
    }
}
