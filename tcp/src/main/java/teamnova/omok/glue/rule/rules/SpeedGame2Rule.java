package teamnova.omok.glue.rule.rules;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionRuleAccess;
import teamnova.omok.glue.rule.Rule;
import teamnova.omok.glue.rule.RuleId;
import teamnova.omok.glue.rule.RuleMetadata;
import teamnova.omok.glue.rule.RuleRuntimeContext;

/**
 * 스피드 게임 2: 총 100초 타이머를 소진하면 해당 플레이어를 패배 처리한다.
 * 호출 시점: 게임 진행 중.
 */
public final class SpeedGame2Rule implements Rule {
    private static final RuleMetadata METADATA = new RuleMetadata(
        RuleId.SPEED_GAME_2,
        900
    );

    @Override
    public RuleMetadata getMetadata() {
        return METADATA;
    }

    @Override
    public void invoke(GameSessionRuleAccess context, RuleRuntimeContext runtime) {
        // Not implemented: enforcing a 100-second total timer needs integration with the
        // timeout scheduler and per-player countdown tracking. The current timeout coordinator
        // only supports per-turn deadlines, so this rule is deferred.
    }
}
