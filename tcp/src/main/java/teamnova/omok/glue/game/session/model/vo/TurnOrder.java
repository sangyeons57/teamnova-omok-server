package teamnova.omok.glue.game.session.model.vo;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Participant order snapshot for a game session turn cycle.
 */
public final class TurnOrder {
    private static final TurnOrder EMPTY = new TurnOrder(List.of());

    private final List<String> userIds;

    private TurnOrder(List<String> userIds) {
        this.userIds = userIds;
    }

    public static TurnOrder empty() {
        return EMPTY;
    }

    public static TurnOrder of(List<String> userIds) {
        Objects.requireNonNull(userIds, "userIds");
        if (userIds.isEmpty()) {
            throw new IllegalArgumentException("userIds must not be empty");
        }
        return new TurnOrder(List.copyOf(userIds));
    }

    public List<String> userIds() {
        return Collections.unmodifiableList(userIds);
    }

    public int size() {
        return userIds.size();
    }

    public String userIdAt(int index) {
        return userIds.get(index);
    }
}

