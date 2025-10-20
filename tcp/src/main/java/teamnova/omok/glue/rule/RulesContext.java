package teamnova.omok.glue.rule;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import teamnova.omok.glue.game.session.interfaces.session.*;
import teamnova.omok.glue.game.session.model.GameSession;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateType;
import teamnova.omok.glue.rule.RuleRuntimeContext;

public class RulesContext {
    private final GameSession session;
    private final List<RuleId> ruleIds;
    private final int lowestScore;
    private final Map<String, Object> data = new ConcurrentHashMap<>();

    private RulesContext(GameSession session,
                         List<RuleId> ruleIds,
                         int lowestScore) {
        this.session = Objects.requireNonNull(session, "session");
        this.ruleIds = List.copyOf(Objects.requireNonNull(ruleIds, "ruleIds"));
        this.lowestScore = lowestScore;
    }

    public static RulesContext fromRules(GameSession session,
                                         List<Rule> rules,
                                         int lowestScore) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(rules, "rules");
        List<RuleId> ids = new ArrayList<>();
        for (Rule rule : rules) {
            RuleMetadata metadata = rule.getMetadata();
            if (metadata == null || metadata.id == null) {
                continue;
            }
            ids.add(metadata.id);
        }
        return new RulesContext(session, ids, lowestScore);
    }

    public <T extends GameSessionAccessInterface> T getSession() {
        return (T)session;
    }

    public Object getData(String key) {
        return data.get(key);
    }

    public int lowestScore() {
        return lowestScore;
    }

    public void putData(String key, Object value) {
        data.put(key, value);
    }

    public boolean allowsActivation(GameSessionStateType type) {
        return !ruleIds.isEmpty();
    }

    public void activateRules(RuleRuntimeContext runtime, GameSessionStateType targetState) {
        Objects.requireNonNull(runtime, "runtime");
        if (!allowsActivation(targetState)) {
            return;
        }
        for (RuleId id : ruleIds) {
            Rule rule = RuleRegistry.getInstance().get(id);
            if (rule != null) {
                rule.invoke(this, runtime);
            }
        }
    }
}
