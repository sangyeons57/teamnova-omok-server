package teamnova.omok.glue.game.session.interfaces.session;

import teamnova.omok.glue.rule.RulesContext;

public interface GameSessionRuleAccess extends GameSessionAccessInterface {
    RulesContext getRulesContext();
    void setRulesContext(RulesContext rulesContext);
    void setLowestParticipantScore(int score);
    void setDesiredRuleCount(int count);
}
