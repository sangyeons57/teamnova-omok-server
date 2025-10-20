package teamnova.omok.glue.rule;


public interface Rule {
    RuleMetadata getMetadata();

    void invoke(RulesContext context, RuleRuntimeContext runtime);
}
