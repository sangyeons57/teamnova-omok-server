package teamnova.omok.support;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionRuleAccess;
import teamnova.omok.glue.game.session.model.vo.GameSessionId;
import teamnova.omok.glue.rule.api.RuleId;

/**
 * Lightweight test double for {@link GameSessionRuleAccess}.
 */
public final class TestRuleAccess implements GameSessionRuleAccess {
    private final GameSessionId sessionId;
    private final ReentrantLock lock = new ReentrantLock();
    private final Map<String, Object> data = new HashMap<>();
    private List<RuleId> ruleIds = new ArrayList<>();

    public TestRuleAccess() {
        this.sessionId = GameSessionId.random();
    }

    @Override
    public GameSessionId sessionId() {
        return sessionId;
    }

    @Override
    public ReentrantLock lock() {
        return lock;
    }

    @Override
    public void setRuleIds(List<RuleId> ruleIds) {
        this.ruleIds = ruleIds == null ? new ArrayList<>() : new ArrayList<>(ruleIds);
    }

    @Override
    public List<RuleId> getRuleIds() {
        return List.copyOf(ruleIds);
    }

    @Override
    public Object getRuleData(String key) {
        return data.get(key);
    }

    @Override
    public void putRuleData(String key, Object value) {
        if (value == null) {
            data.remove(key);
        } else {
            data.put(key, value);
        }
    }

    @Override
    public void clearRuleData() {
        data.clear();
    }

    @Override
    public boolean isRuleEmpty() {
        return ruleIds.isEmpty();
    }
}
