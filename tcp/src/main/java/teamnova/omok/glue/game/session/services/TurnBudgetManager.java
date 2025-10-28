package teamnova.omok.glue.game.session.services;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionRuleAccess;

/**
 * Tracks per-player cumulative turn budgets for rules that enforce shared time limits.
 */
public final class TurnBudgetManager {
    private static final String BUDGET_KEY = "rules.turnBudget.remaining";

    public Map<String, Long> snapshot(GameSessionRuleAccess access) {
        Objects.requireNonNull(access, "access");
        Map<String, Long> view = budgets(access, false);
        if (view == null || view.isEmpty()) {
            return Map.of();
        }
        return Map.copyOf(view);
    }

    public long remaining(GameSessionRuleAccess access, String userId) {
        Objects.requireNonNull(access, "access");
        Objects.requireNonNull(userId, "userId");
        Map<String, Long> view = budgets(access, false);
        if (view == null) {
            return 0L;
        }
        return view.getOrDefault(userId, 0L);
    }

    public void update(GameSessionRuleAccess access, String userId, long remainingMillis) {
        Objects.requireNonNull(access, "access");
        Objects.requireNonNull(userId, "userId");
        Map<String, Long> view = budgets(access, true);
        view.put(userId, remainingMillis);
    }

    public long decrement(GameSessionRuleAccess access, String userId, long millis) {
        Objects.requireNonNull(access, "access");
        Objects.requireNonNull(userId, "userId");
        if (millis <= 0L) {
            return remaining(access, userId);
        }
        Map<String, Long> view = budgets(access, true);
        long remaining = view.getOrDefault(userId, 0L) - millis;
        if (remaining < 0L) {
            remaining = 0L;
        }
        view.put(userId, remaining);
        return remaining;
    }

    public void clear(GameSessionRuleAccess access) {
        Objects.requireNonNull(access, "access");
        Map<String, Long> view = budgets(access, false);
        if (view != null) {
            view.clear();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Long> budgets(GameSessionRuleAccess access, boolean createIfMissing) {
        Object stored = access.getRuleData(BUDGET_KEY);
        if (stored instanceof Map<?, ?> map) {
            return (Map<String, Long>) map;
        }
        if (!createIfMissing) {
            return null;
        }
        Map<String, Long> created = new LinkedHashMap<>();
        access.putRuleData(BUDGET_KEY, created);
        return created;
    }
}
