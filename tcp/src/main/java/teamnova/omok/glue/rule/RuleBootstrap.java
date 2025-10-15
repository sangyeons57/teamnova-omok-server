package teamnova.omok.glue.rule;

import java.util.Objects;

import teamnova.omok.glue.rule.rules.EveryFiveTurnBlockerRule;
import teamnova.omok.glue.rule.rules.PerTurnAdjacentBlockerRule;
import teamnova.omok.glue.rule.rules.EveryTwoTurnJokerRule;
import teamnova.omok.glue.rule.rules.GoCaptureRule;
import teamnova.omok.glue.rule.rules.RotatePlayerStonesRule;

public class RuleBootstrap {

    public void registerDefaults(RuleRegistry ruleRegistry) {
        Objects.requireNonNull(ruleRegistry, "ruleRegistry");
        ruleRegistry.register(new EveryFiveTurnBlockerRule());
        ruleRegistry.register(new PerTurnAdjacentBlockerRule());
        ruleRegistry.register(new EveryTwoTurnJokerRule());
        ruleRegistry.register(new GoCaptureRule());
        ruleRegistry.register(new RotatePlayerStonesRule());
    }
}
