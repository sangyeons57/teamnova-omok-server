package teamnova.omok.glue.rule.rules;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionRuleAccess;
import teamnova.omok.glue.rule.Rule;
import teamnova.omok.glue.rule.RuleId;
import teamnova.omok.glue.rule.RuleMetadata;
import teamnova.omok.glue.rule.RuleRuntimeContext;

/**
 * 새로운 플레이어: 비플레이어 돌을 가상의 플레이어 소유로 간주하여 승패를 판정한다.
 * 호출 시점: 돌 배치 후.
 */
public final class NewPlayerRule implements Rule {
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
        // Not implemented: supporting a virtual player that claims neutral stones
        // requires extending outcome evaluation, scoring, and messaging to accept
        // dynamic participant IDs. The current session model assumes a fixed player
        // roster, so this rule is left inert until the lifecycle is reworked.
    }
}
