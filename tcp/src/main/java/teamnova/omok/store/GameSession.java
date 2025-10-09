package teamnova.omok.store;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Represents an in-game session with participants and mutable runtime state.
 */
public class GameSession {
    public static final int BOARD_WIDTH = BoardStore.DEFAULT_WIDTH;
    public static final int BOARD_HEIGHT = BoardStore.DEFAULT_HEIGHT;
    public static final long TURN_DURATION_MILLIS = 15_000L;

    private final UUID id;
    private final List<String> userIds;
    private final long createdAt;

    private final ReentrantLock lock = new ReentrantLock();
    private final Map<String, Boolean> readyStates = new ConcurrentHashMap<>();
    private final BoardStore boardStore;
    private final TurnStore turnStore;

    private volatile boolean gameStarted;
    private volatile long gameStartedAt;

    public GameSession(List<String> userIds) {
        Objects.requireNonNull(userIds, "userIds");
        if (userIds.isEmpty()) {
            throw new IllegalArgumentException("userIds must not be empty");
        }
        this.id = UUID.randomUUID();
        this.userIds = List.copyOf(userIds);
        this.createdAt = System.currentTimeMillis();
        this.boardStore = new BoardStore(BOARD_WIDTH, BOARD_HEIGHT);
        this.turnStore = new TurnStore();

        for (String userId : this.userIds) {
            readyStates.put(userId, Boolean.FALSE);
        }
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

    public ReentrantLock lock() {
        return lock;
    }

    public BoardStore getBoardStore() {
        return boardStore;
    }

    public TurnStore getTurnStore() {
        return turnStore;
    }

    public boolean isGameStarted() {
        return gameStarted;
    }

    public long getGameStartedAt() {
        return gameStartedAt;
    }

    public void markGameStarted(long startedAt) {
        this.gameStarted = true;
        this.gameStartedAt = startedAt;
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

    public Map<String, Boolean> readyStatesView() {
        return Collections.unmodifiableMap(readyStates);
    }

    public int playerIndexOf(String userId) {
        return userIds.indexOf(userId);
    }

    public boolean containsUser(String userId) {
        return readyStates.containsKey(userId);
    }
}
