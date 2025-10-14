package teamnova.omok.glue.rule;

import java.util.Set;

public class RuleMetadata {
    public final RuleId id;
    public final Set<RuleType> types;
    public final int limitScore;


    public RuleMetadata(RuleId id, Set<RuleType> types, int limitScore) {
        this.id = id;
        this.types = Set.copyOf(types);
        this.limitScore = limitScore;
    }
}
