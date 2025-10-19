package teamnova.omok.glue.game.session.model;

import teamnova.omok.glue.rule.RulesContext;

/**
 * Holds mutable rule-selection state for a session.
 */
public final class RulesStore {
    private volatile RulesContext rulesContext;
    private volatile int lowestParticipantScore;
    private volatile int desiredRuleCount;

    public RulesContext rulesContext() {
        return rulesContext;
    }

    public void rulesContext(RulesContext rulesContext) {
        this.rulesContext = rulesContext;
    }

    public int lowestParticipantScore() {
        return lowestParticipantScore;
    }

    public void lowestParticipantScore(int lowestParticipantScore) {
        this.lowestParticipantScore = lowestParticipantScore;
    }

    public int desiredRuleCount() {
        return desiredRuleCount;
    }

    public void desiredRuleCount(int desiredRuleCount) {
        this.desiredRuleCount = desiredRuleCount;
    }
}
