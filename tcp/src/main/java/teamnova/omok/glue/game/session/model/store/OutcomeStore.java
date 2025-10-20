package teamnova.omok.glue.game.session.model.store;

import teamnova.omok.glue.game.session.model.PlayerResult;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds per-player outcome state for an in-progress game session.
 */
public final class OutcomeStore {
    private final Map<String, PlayerResult> playerResults = new ConcurrentHashMap<>();

    public OutcomeStore(List<String> userIds) {
        Objects.requireNonNull(userIds, "userIds");
        reset(userIds);
    }

    public void reset(List<String> userIds) {
        Objects.requireNonNull(userIds, "userIds");
        playerResults.clear();
        for (String userId : userIds) {
            playerResults.put(userId, PlayerResult.PENDING);
        }
    }

    public Map<String, PlayerResult> resultsView() {
        return Collections.unmodifiableMap(new HashMap<>(playerResults));
    }

    public PlayerResult resultFor(String userId) {
        return playerResults.getOrDefault(userId, PlayerResult.PENDING);
    }

    public boolean updateResult(String userId, PlayerResult result) {
        if (userId == null || result == null) {
            return false;
        }
        PlayerResult previous = playerResults.put(userId, result);
        return !Objects.equals(previous, result);
    }

    public boolean isResolved() {
        return playerResults.values().stream()
            .allMatch(result -> result != PlayerResult.PENDING);
    }
}
