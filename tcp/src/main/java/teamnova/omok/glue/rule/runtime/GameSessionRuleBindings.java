package teamnova.omok.glue.rule.runtime;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import teamnova.omok.glue.rule.api.Rule;

/**
 * Captures the resolved rule instances that belong to a session and exposes
 * capability-specific projections without repeated registry lookups.
 */
public final class GameSessionRuleBindings {
    private static final GameSessionRuleBindings EMPTY = new GameSessionRuleBindings(List.of());

    private final List<Rule> orderedRules;
    private final Map<Class<?>, List<?>> capabilityCache = new ConcurrentHashMap<>();

    private GameSessionRuleBindings(List<Rule> orderedRules) {
        this.orderedRules = List.copyOf(orderedRules);
    }

    public static GameSessionRuleBindings empty() {
        return EMPTY;
    }

    public static GameSessionRuleBindings of(List<Rule> rules) {
        if (rules == null || rules.isEmpty()) {
            return EMPTY;
        }
        return new GameSessionRuleBindings(rules);
    }

    public List<Rule> orderedRules() {
        return orderedRules;
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> capability(Class<T> capabilityType) {
        Objects.requireNonNull(capabilityType, "capabilityType");
        if (orderedRules.isEmpty()) {
            return List.of();
        }
        return (List<T>) capabilityCache.computeIfAbsent(capabilityType, key -> {
            List<T> matches = new ArrayList<>();
            for (Rule rule : orderedRules) {
                if (capabilityType.isInstance(rule)) {
                    matches.add(capabilityType.cast(rule));
                }
            }
            return List.copyOf(matches);
        });
    }
}
