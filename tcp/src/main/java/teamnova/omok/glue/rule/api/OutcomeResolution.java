package teamnova.omok.glue.rule.api;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import teamnova.omok.glue.game.session.model.PlayerResult;

/**
 * Represents outcome adjustments suggested by a rule. The assignments map is
 * keyed by user id and stores the desired result for that player. The
 * {@code finalizeNow} flag can be used by rules that wish to force resolution
 * even if not every player received an explicit assignment.
 */
public final class OutcomeResolution {
    private static final OutcomeResolution EMPTY = new OutcomeResolution(Map.of(), false);

    private final Map<String, PlayerResult> assignments;
    private final boolean finalizeNow;

    private OutcomeResolution(Map<String, PlayerResult> assignments, boolean finalizeNow) {
        this.assignments = assignments;
        this.finalizeNow = finalizeNow;
    }

    public static OutcomeResolution empty() {
        return EMPTY;
    }

    public static OutcomeResolution of(Map<String, PlayerResult> assignments, boolean finalizeNow) {
        if (assignments == null || assignments.isEmpty()) {
            return finalizeNow ? new OutcomeResolution(Map.of(), true) : EMPTY;
        }
        Map<String, PlayerResult> copy = new LinkedHashMap<>();
        assignments.forEach((userId, result) -> {
            if (userId != null && result != null) {
                copy.put(userId, result);
            }
        });
        if (copy.isEmpty() && !finalizeNow) {
            return EMPTY;
        }
        return new OutcomeResolution(Collections.unmodifiableMap(copy), finalizeNow);
    }

    public Map<String, PlayerResult> assignments() {
        return assignments;
    }

    public boolean finalizeNow() {
        return finalizeNow;
    }

    public boolean isEmpty() {
        return assignments.isEmpty() && !finalizeNow;
    }
}
