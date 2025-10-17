package teamnova.omok.glue.store;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import teamnova.omok.glue.game.PlayerResult;
import teamnova.omok.glue.game.PostGameDecision;
import teamnova.omok.glue.rule.RulesContext;
import teamnova.omok.glue.service.MysqlService;
import teamnova.omok.glue.service.ServiceManager;

/**
 * Represents an in-game session with participants and mutable runtime state.
 */
public class GameSession {
    public static final int BOARD_WIDTH = BoardStore.DEFAULT_WIDTH;
    public static final int BOARD_HEIGHT = BoardStore.DEFAULT_HEIGHT;
    public static final long TURN_DURATION_MILLIS = 15_000L;
    public static final long POST_GAME_DECISION_DURATION_MILLIS = 30_000L;

    private final UUID id;
    private final List<User> users;
    private final long createdAt;

    private final ReentrantLock lock = new ReentrantLock();
    private final Map<String, Boolean> readyStates = new ConcurrentHashMap<>();
    // Users who dropped before the game finished or chose to leave during post-game decisions.
    private final Set<String> disconnectedUserIds = ConcurrentHashMap.newKeySet();
    // Tracks players who opted into a rematch during the post-game window.
    private final BoardStore boardStore;
    private final TurnStore turnStore;
    private final OutcomeStore outcomeStore;
    private final Map<String, PostGameDecision> postGameDecisions = new ConcurrentHashMap<>();
    private final Set<String> rematchRequestUserIds = ConcurrentHashMap.newKeySet();
    private volatile RulesContext rulesContext;
    // Lowest participant score captured at session creation for rule selection
    private volatile int lowestParticipantScore;
    // Deterministically chosen rule count based on lowest score
    private volatile int desiredRuleCount;

    private volatile boolean gameStarted;
    private volatile long gameStartedAt;
    private volatile long gameEndedAt;
    private volatile int completedTurnCount;

    public GameSession(List<String> userIds) {
        Objects.requireNonNull(userIds, "userIds");
        if (userIds.isEmpty()) {
            throw new IllegalArgumentException("userIds must not be empty");
        }
        this.id = UUID.randomUUID();
        this.createdAt = System.currentTimeMillis();
        this.boardStore = new BoardStore(BOARD_WIDTH, BOARD_HEIGHT);
        this.turnStore = new TurnStore();
        this.outcomeStore = new OutcomeStore(userIds);
        this.rulesContext = null;

        this.users = Collections.synchronizedList(new java.util.ArrayList<>());

        for (String userId : userIds) {
            readyStates.put(userId, Boolean.FALSE);
            this.users.add(
                    ServiceManager.getInstance().getMysqlService().findUser(userId, new User("", "", 0, User.Status.INACTIVE, 0))
            );
        }

    }

    public UUID getId() {
        return id;
    }

    public List<User> getUsers() {
        return Collections.unmodifiableList(users);
    }
    public List<String> getUserIds() {
        return Collections.unmodifiableList(users.stream().map(User::id).toList());
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

    public RulesContext getRulesContext() {
        return rulesContext;
    }

    public void setRulesContext(RulesContext rulesContext) {
        this.rulesContext = rulesContext;
    }

    public int getLowestParticipantScore() { return lowestParticipantScore; }
    public void setLowestParticipantScore(int score) { this.lowestParticipantScore = score; }
    public int getDesiredRuleCount() { return desiredRuleCount; }
    public void setDesiredRuleCount(int count) { this.desiredRuleCount = count; }

    public boolean isGameStarted() {
        return gameStarted;
    }

    public long getGameStartedAt() {
        return gameStartedAt;
    }

    public void markGameStarted(long startedAt) {
        this.gameStarted = true;
        this.gameStartedAt = startedAt;
        this.gameEndedAt = 0L;
        this.completedTurnCount = 0;
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

    public boolean markDisconnected(String userId) {
        return disconnectedUserIds.add(userId);
    }

    public boolean clearDisconnected(String userId) {
        return disconnectedUserIds.remove(userId);
    }

    public Set<String> disconnectedUsersView() {
        return Collections.unmodifiableSet(disconnectedUserIds);
    }

    public void resetDisconnectedUsers() {
        disconnectedUserIds.clear();
    }

    public boolean recordPostGameDecision(String userId, PostGameDecision decision) {
        if (userId == null || decision == null || !containsUser(userId)) {
            return false;
        }
        PostGameDecision previous = postGameDecisions.putIfAbsent(userId, decision);
        if (previous != null) {
            return false;
        }
        if (decision == PostGameDecision.REMATCH) {
            rematchRequestUserIds.add(userId);
        } else {
            rematchRequestUserIds.remove(userId);
            markDisconnected(userId);
        }
        return true;
    }

    public boolean hasPostGameDecision(String userId) {
        return postGameDecisions.containsKey(userId);
    }

    public PostGameDecision decisionFor(String userId) {
        return postGameDecisions.get(userId);
    }

    public Map<String, PostGameDecision> postGameDecisionsView() {
        return Collections.unmodifiableMap(postGameDecisions);
    }

    public Set<String> rematchRequestsView() {
        return Collections.unmodifiableSet(rematchRequestUserIds);
    }

    public void resetPostGameDecisions() {
        postGameDecisions.clear();
        rematchRequestUserIds.clear();
    }

    public int playerIndexOf(String userId) {
        return getUserIds().indexOf(userId);
    }

    public boolean containsUser(String userId) {
        return readyStates.containsKey(userId);
    }

    public void resetOutcomes() {
        outcomeStore.reset(getUserIds());
    }

    public OutcomeStore getOutcomeStore() {
        return outcomeStore;
    }

    public PlayerResult outcomeFor(String userId) {
        return outcomeStore.resultFor(userId);
    }

    public boolean updateOutcome(String userId, PlayerResult result) {
        return outcomeStore.updateResult(userId, result);
    }

    public boolean isGameFinished() {
        return outcomeStore.isResolved();
    }

    public long getGameEndedAt() {
        return gameEndedAt;
    }

    public void markGameFinished(long endedAt, int turnCount) {
        if (endedAt > 0) {
            if (this.gameEndedAt == 0L || endedAt > this.gameEndedAt) {
                this.gameEndedAt = endedAt;
            }
        }
        if (turnCount > 0) {
            if (turnCount > this.completedTurnCount) {
                this.completedTurnCount = turnCount;
            }
        }
    }

    public int getCompletedTurnCount() {
        return completedTurnCount > 0 ? completedTurnCount : turnStore.getTurnNumber();
    }

    public long getGameDurationMillis() {
        long start = gameStartedAt;
        long end = gameEndedAt;
        if (start <= 0 || end <= 0 || end < start) {
            return 0L;
        }
        return end - start;
    }
}
