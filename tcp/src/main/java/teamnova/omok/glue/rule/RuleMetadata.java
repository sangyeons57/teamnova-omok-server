package teamnova.omok.glue.rule;

public class RuleMetadata {
    public final RuleId id;
    public final int limitScore;

    public RuleMetadata(RuleId id, int limitScore) {
        this.id = id;
        this.limitScore = limitScore;
    }
}
