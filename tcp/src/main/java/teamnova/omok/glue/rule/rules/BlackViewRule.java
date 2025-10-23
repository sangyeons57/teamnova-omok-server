package teamnova.omok.glue.rule.rules;

import java.util.Arrays;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionRuleAccess;
import teamnova.omok.glue.game.session.model.Stone;
import teamnova.omok.glue.rule.api.BoardSnapshotTransformingRule;
import teamnova.omok.glue.rule.api.Rule;
import teamnova.omok.glue.rule.api.RuleId;
import teamnova.omok.glue.rule.api.RuleMetadata;
import teamnova.omok.glue.rule.runtime.RuleRuntimeContext;
import teamnova.omok.glue.rule.api.RuleTriggerKind;

/**
 * 난 검은색이 좋아: 모든 플레이어의 돌이 검은 돌처럼 보이도록 스냅샷을 변환한다.
 * 호출 시점: 게임 시작 시 1회 활성화 플래그를 기록한다.
 */
public final class BlackViewRule implements Rule, BoardSnapshotTransformingRule {
    public static final String ACTIVE_KEY = "rule:blackView:active";

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
        if (access == null || runtime == null) {
            return;
        }
        if (runtime.triggerKind() != RuleTriggerKind.GAME_START) {
            return;
        }
        if (Boolean.TRUE.equals(access.getRuleData(ACTIVE_KEY))) {
            return;
        }
        access.putRuleData(ACTIVE_KEY, Boolean.TRUE);
    }

    @Override
    public byte[] transformBoardSnapshot(GameSessionRuleAccess access, byte[] snapshot) {
        if (!Boolean.TRUE.equals(access.getRuleData(ACTIVE_KEY))) {
            return snapshot;
        }
        if (snapshot == null || snapshot.length == 0) {
            return snapshot;
        }
        byte[] masked = Arrays.copyOf(snapshot, snapshot.length);
        for (int i = 0; i < masked.length; i++) {
            Stone stone = Stone.fromByte(masked[i]);
            if (stone.isPlayerStone()) {
                masked[i] = Stone.PLAYER1.code();
            }
        }
        return masked;
    }
}
