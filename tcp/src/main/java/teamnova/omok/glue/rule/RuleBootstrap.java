package teamnova.omok.glue.rule;

import java.util.Objects;

import teamnova.omok.glue.rule.rules.EveryFiveTurnBlockerRule;

public class RuleBootstrap {

    public void registerDefaults(RuleRegistry ruleRegistry) {
        Objects.requireNonNull(ruleRegistry, "ruleRegistry");
        ruleRegistry.register(new EveryFiveTurnBlockerRule());
    }
}
