package teamnova.omok.glue.rule;


public interface Rule {
    public RuleMetadata getMetadata();

    default public void invoke(RulesContext context) { }
}
