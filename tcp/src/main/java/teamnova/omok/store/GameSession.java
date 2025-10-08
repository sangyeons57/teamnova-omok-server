package teamnova.omok.store;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Represents an in-game session consisting of matched users and basic game metadata.
 */
public class GameSession {
    private final UUID id;
    private final List<String> userIds;
    private final long createdAt;

    public GameSession(List<String> userIds) {
        this.id = UUID.randomUUID();
        this.userIds = List.copyOf(userIds);
        this.createdAt = System.currentTimeMillis();
    }

    public UUID getId() {
        return id;
    }

    public List<String> getUserIds() {
        return Collections.unmodifiableList(userIds);
    }

    public long getCreatedAt() {
        return createdAt;
    }
}
