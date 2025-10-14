package teamnova.omok.domain.rule;

import java.util.Objects;

import teamnova.omok.domain.rule.model.rules.EveryFiveTurnBlockerRule;

public class RuleBootstrap {

    public void registerDefaults(RuleRegistry ruleRegistry) {
        Objects.requireNonNull(ruleRegistry, "ruleRegistry");
        ruleRegistry.register(new EveryFiveTurnBlockerRule());
    }
}
