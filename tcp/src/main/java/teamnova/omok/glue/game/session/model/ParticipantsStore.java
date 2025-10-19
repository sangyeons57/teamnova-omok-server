package teamnova.omok.glue.game.session.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import teamnova.omok.glue.data.model.UserData;

/**
 * Maintains participant snapshots and lightweight per-user flags.
 */
public final class ParticipantsStore {
    private final List<UserData> participants;
    private final List<String> participantIds;
    private final Map<String, Boolean> readyStates = new ConcurrentHashMap<>();
    private final Set<String> disconnectedUserIds = ConcurrentHashMap.newKeySet();

    public ParticipantsStore(List<UserData> participants) {
        Objects.requireNonNull(participants, "participants");
        if (participants.isEmpty()) {
            throw new IllegalArgumentException("participants must not be empty");
        }
        this.participants = Collections.unmodifiableList(List.copyOf(participants));
        this.participantIds = Collections.unmodifiableList(
                this.participants.stream().map(UserData::id).toList()
        );
        for (String userId : participantIds) {
            readyStates.put(userId, Boolean.FALSE);
        }
    }

    public List<UserData> participants() {
        return participants;
    }

    public List<String> participantIds() {
        return participantIds;
    }

    public boolean contains(String userId) {
        return readyStates.containsKey(userId);
    }

    public int indexOf(String userId) {
        return participantIds.indexOf(userId);
    }

    public boolean isReady(String userId) {
        return Boolean.TRUE.equals(readyStates.get(userId));
    }

    public boolean markReady(String userId) {
        Boolean previous = readyStates.put(userId, Boolean.TRUE);
        return !Boolean.TRUE.equals(previous);
    }

    public boolean allReady() {
        return readyStates.values().stream().allMatch(Boolean::booleanValue);
    }

    public Map<String, Boolean> readinessView() {
        return Collections.unmodifiableMap(readyStates);
    }

    public boolean markDisconnected(String userId) {
        return disconnectedUserIds.add(userId);
    }

    public boolean clearDisconnected(String userId) {
        return disconnectedUserIds.remove(userId);
    }

    public void resetDisconnected() {
        disconnectedUserIds.clear();
    }

    public Set<String> disconnectedView() {
        return Collections.unmodifiableSet(disconnectedUserIds);
    }
}
