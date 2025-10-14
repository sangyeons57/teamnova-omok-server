package teamnova.omok.domain.rule.model;


import teamnova.omok.domain.rule.RulesContext;

public interface Rule {
    public RuleMetadata getMetadata();

    default public void invoke(RulesContext context) { }
}
