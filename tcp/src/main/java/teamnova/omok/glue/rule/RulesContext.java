package teamnova.omok.glue.rule;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import teamnova.omok.glue.game.session.interfaces.session.*;
import teamnova.omok.glue.game.session.model.GameSession;
import teamnova.omok.glue.game.session.model.dto.GameSessionServices;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContext;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateType;

public class RulesContext {
    private final GameSession session;
    private final List<RuleId> ruleIds;
    private final int lowestScore;
    private final Map<String, Object> data = new ConcurrentHashMap<>();

    private volatile GameSessionStateContext stateContext;
    private volatile GameSessionServices services;

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

    public GameSessionStateContext stateContext() {
        return stateContext;
    }

    public void attachStateContext(GameSessionStateContext stateContext) {
        this.stateContext = stateContext;
    }

    public GameSessionServices services() {
        return services;
    }

    public void attachServices(GameSessionServices services) {
        this.services = services;
    }

    // With simplified rules, we ignore the specific state and always allow activation when any rule exists.
    public boolean setCurrentRuleByGameState(GameSessionStateType type) {
        return !ruleIds.isEmpty();
    }

    private void activateRule(RuleId id) {
        if (id == null) {
            return;
        }
        Rule rule = RuleRegistry.getInstance().get(id);
        if (rule == null) {
            return;
        }
        rule.invoke(this);
    }

    public void activateCurrentRule() {
        for (RuleId id : ruleIds) {
            activateRule(id);
        }
    }
}
