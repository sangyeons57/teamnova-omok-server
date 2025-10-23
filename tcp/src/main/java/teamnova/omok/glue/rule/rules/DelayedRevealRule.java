package teamnova.omok.glue.rule.rules;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionRuleAccess;
import teamnova.omok.glue.rule.api.Rule;
import teamnova.omok.glue.rule.api.RuleId;
import teamnova.omok.glue.rule.api.RuleMetadata;
import teamnova.omok.glue.rule.runtime.RuleRuntimeContext;

/**
 * 시간차 소환: 돌을 숨겼다가 한 턴 뒤에 공개하며, 충돌 시 최근 돌로 대체한다.
 * 호출 시점: 턴 시작 시 및 돌 배치 후.
 */
public final class DelayedRevealRule implements Rule {
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
        // Not implemented: buffering hidden placements requires a staging board and custom
        // messaging so clients can render concealed stones. The session pipeline exposes
        // only immediate placements, so this rule is currently a placeholder.
    }
}
