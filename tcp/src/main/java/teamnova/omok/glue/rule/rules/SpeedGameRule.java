package teamnova.omok.glue.rule.rules;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionRuleAccess;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionTurnAccess;
import teamnova.omok.glue.game.session.model.dto.GameSessionServices;
import teamnova.omok.glue.game.session.model.vo.TurnTiming;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContext;
import teamnova.omok.glue.rule.api.Rule;
import teamnova.omok.glue.rule.api.RuleId;
import teamnova.omok.glue.rule.api.RuleMetadata;
import teamnova.omok.glue.rule.api.TurnTimingRule;
import teamnova.omok.glue.rule.runtime.RuleRuntimeContext;
import teamnova.omok.glue.rule.api.RuleTriggerKind;

/**
 * 스피드 게임: 게임 시작 시 턴 제한 시간을 5초로 축소한다.
 */
public final class SpeedGameRule implements Rule, TurnTimingRule {
    private static final RuleMetadata METADATA = new RuleMetadata(
        RuleId.SPEED_GAME,
        0
    );

    private static final long FAST_DURATION_MILLIS = 5_000L;
    private static final String APPLIED_KEY = "rule:speedGame:applied";

    @Override
    public RuleMetadata getMetadata() {
        return METADATA;
    }

    @Override
    public void invoke(GameSessionRuleAccess context, RuleRuntimeContext runtime) {
        if (context == null || runtime == null) {
            return;
        }
        if (runtime.triggerKind() != RuleTriggerKind.GAME_START) {
            return;
        }
        adjustTurnTiming(context, runtime);
    }

    @Override
    public boolean adjustTurnTiming(GameSessionRuleAccess context, RuleRuntimeContext runtime) {
        if (context == null || runtime == null) {
            return false;
        }
        if (Boolean.TRUE.equals(context.getRuleData(APPLIED_KEY))) {
            return false;
        }
        GameSessionStateContext stateContext = runtime.stateContext();
        GameSessionServices services = runtime.services();
        if (stateContext == null || services == null) {
            return false;
        }

        GameSessionTurnAccess turns = stateContext.turns();
        try {
            turns.durationMillis(FAST_DURATION_MILLIS);
        } catch (IllegalArgumentException ignored) {
            return false;
        }

        TurnTiming currentTiming = turns.timing();
        long startAt = currentTiming != null ? currentTiming.startAt() : 0L;
        if (startAt <= 0L) {
            startAt = System.currentTimeMillis();
        }
        TurnTiming updatedTiming = TurnTiming.of(startAt, startAt + FAST_DURATION_MILLIS);
        turns.timing(updatedTiming);

        context.putRuleData(APPLIED_KEY, Boolean.TRUE);
        return true;
    }
}
