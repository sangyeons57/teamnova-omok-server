package teamnova.omok.rule;


public interface Rule {
    public RuleMetadata getMetadata();

    default public void invoke(RulesContext context) { }
}
