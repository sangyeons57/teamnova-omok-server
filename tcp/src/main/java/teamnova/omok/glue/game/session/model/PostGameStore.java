package teamnova.omok.glue.game.session.model;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks post-game decisions and rematch intent.
 */
public final class PostGameStore {
    private final Map<String, PostGameDecision> decisions = new ConcurrentHashMap<>();
    private final Set<String> rematchRequestUserIds = ConcurrentHashMap.newKeySet();

    public boolean recordDecision(String userId, PostGameDecision decision) {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(decision, "decision");
        PostGameDecision previous = decisions.putIfAbsent(userId, decision);
        if (previous != null) {
            return false;
        }
        if (decision == PostGameDecision.REMATCH) {
            rematchRequestUserIds.add(userId);
        } else {
            rematchRequestUserIds.remove(userId);
        }
        return true;
    }

    public boolean hasDecision(String userId) {
        return decisions.containsKey(userId);
    }

    public PostGameDecision decisionFor(String userId) {
        return decisions.get(userId);
    }

    public Map<String, PostGameDecision> decisionsView() {
        return Collections.unmodifiableMap(decisions);
    }

    public Set<String> rematchRequestsView() {
        return Collections.unmodifiableSet(rematchRequestUserIds);
    }

    public void reset() {
        decisions.clear();
        rematchRequestUserIds.clear();
    }
}
