package teamnova.omok.glue.game.session.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionRuleAccess;
import teamnova.omok.glue.game.session.model.Stone;

/**
 * Coordinates hidden stone placements that are buffered by hidden placement rules.
 */
public final class HiddenPlacementCoordinator {
    private static final String BUFFER_KEY = "rules.hiddenPlacements";

    public void queue(GameSessionRuleAccess access, HiddenPlacement placement) {
        Objects.requireNonNull(access, "access");
        Objects.requireNonNull(placement, "placement");
        List<HiddenPlacement> buffer = ensureBuffer(access);
        Iterator<HiddenPlacement> iterator = buffer.iterator();
        while (iterator.hasNext()) {
            HiddenPlacement existing = iterator.next();
            if (existing.index == placement.index) {
                iterator.remove();
            }
        }
        buffer.add(placement);
    }

    public List<HiddenPlacement> drain(GameSessionRuleAccess access) {
        Objects.requireNonNull(access, "access");
        List<HiddenPlacement> buffer = buffer(access);
        if (buffer == null || buffer.isEmpty()) {
            return List.of();
        }
        List<HiddenPlacement> snapshot = new ArrayList<>(buffer);
        buffer.clear();
        return snapshot;
    }

    public List<HiddenPlacement> snapshot(GameSessionRuleAccess access) {
        Objects.requireNonNull(access, "access");
        List<HiddenPlacement> buffer = buffer(access);
        if (buffer == null || buffer.isEmpty()) {
            return List.of();
        }
        return Collections.unmodifiableList(new ArrayList<>(buffer));
    }

    public boolean hasHiddenPlacements(GameSessionRuleAccess access) {
        Objects.requireNonNull(access, "access");
        List<HiddenPlacement> buffer = buffer(access);
        return buffer != null && !buffer.isEmpty();
    }

    public void clear(GameSessionRuleAccess access) {
        Objects.requireNonNull(access, "access");
        List<HiddenPlacement> buffer = buffer(access);
        if (buffer != null) {
            buffer.clear();
        }
    }

    @SuppressWarnings("unchecked")
    private List<HiddenPlacement> buffer(GameSessionRuleAccess access) {
        Object stored = access.getRuleData(BUFFER_KEY);
        if (stored instanceof List<?> list) {
            return (List<HiddenPlacement>) list;
        }
        return null;
    }

    private List<HiddenPlacement> ensureBuffer(GameSessionRuleAccess access) {
        List<HiddenPlacement> buffer = buffer(access);
        if (buffer != null) {
            return buffer;
        }
        List<HiddenPlacement> created = new ArrayList<>();
        access.putRuleData(BUFFER_KEY, created);
        return created;
    }

    public record HiddenPlacement(String userId,
                                  int x,
                                  int y,
                                  int index,
                                  Stone stone,
                                  long requestedAtMillis,
                                  long requestId,
                                  int revealAtTurn) {
        public HiddenPlacement {
            Objects.requireNonNull(userId, "userId");
            Objects.requireNonNull(stone, "stone");
            if (index < 0) {
                throw new IllegalArgumentException("index must not be negative");
            }
            if (revealAtTurn < 0) {
                throw new IllegalArgumentException("revealAtTurn must not be negative");
            }
        }
    }
}
