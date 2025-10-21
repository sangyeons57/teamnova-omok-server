package teamnova.omok.glue.rule.rules;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionRuleAccess;
import teamnova.omok.glue.rule.Rule;
import teamnova.omok.glue.rule.RuleId;
import teamnova.omok.glue.rule.RuleMetadata;
import teamnova.omok.glue.rule.RuleRuntimeContext;

/**
 * 난 검은색이 좋아: 모든 돌이 검은색으로 표시되도록 시야를 변경한다.
 * 호출 시점: 게임 진행 중.
 */
public final class BlackViewRule implements Rule {
    private static final RuleMetadata METADATA = new RuleMetadata(
        RuleId.BLACK_VIEW,
        1_500
    );

    @Override
    public RuleMetadata getMetadata() {
        return METADATA;
    }

    @Override
    public void invoke(GameSessionRuleAccess access, RuleRuntimeContext runtime) {
        // Not implemented: the server currently sends raw stone encodings and has no
        // rendering override hooks per player. Making every stone appear black would
        // require protocol changes or client-side support, so the rule remains a no-op.
    }
}
