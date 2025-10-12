package teamnova.omok.rule;

public class RuleBootstrap {

    public RuleBootstrap() {
        RuleRegistry ruleRegistry = RuleRegistry.getInstance();

        ruleRegistry.register(new Rule);

    }
}
