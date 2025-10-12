package teamnova.omok.rule;

import java.util.Objects;

import teamnova.omok.rule.rules.EveryFiveTurnBlockerRule;

public class RuleBootstrap {

    public void registerDefaults(RuleRegistry ruleRegistry) {
        Objects.requireNonNull(ruleRegistry, "ruleRegistry");
        ruleRegistry.register(new EveryFiveTurnBlockerRule());
    }
}
