package teamnova.omok.glue.game.session.model;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

import teamnova.omok.glue.data.model.UserData;
import teamnova.omok.glue.manager.DataManager;
import teamnova.omok.glue.game.session.model.vo.GameSessionId;
import teamnova.omok.glue.rule.RulesContext;

/**
 * Represents an in-game session with participants and mutable runtime state.
 */
public class GameSession {
    public static final int BOARD_WIDTH = BoardStore.DEFAULT_WIDTH;
    public static final int BOARD_HEIGHT = BoardStore.DEFAULT_HEIGHT;
    public static final long TURN_DURATION_MILLIS = 15_000L;
    public static final long POST_GAME_DECISION_DURATION_MILLIS = 30_000L;

    private final GameSessionId id;
    private final ParticipantsStore participantsStore;
    private final LifecycleStore lifecycleStore;
    private final RulesStore rulesStore = new RulesStore();
    private final BoardStore boardStore;
    private final TurnStore turnStore;
    private final OutcomeStore outcomeStore;
    private final PostGameStore postGameStore = new PostGameStore();
    private final ReentrantLock lock = new ReentrantLock();

    public GameSession(List<String> userIds) {
        Objects.requireNonNull(userIds, "userIds");
        if (userIds.isEmpty()) {
            throw new IllegalArgumentException("userIds must not be empty");
        }
        this.id = GameSessionId.random();
        this.lifecycleStore = new LifecycleStore(System.currentTimeMillis());
        this.boardStore = new BoardStore(BOARD_WIDTH, BOARD_HEIGHT);
        this.turnStore = new TurnStore();
        this.outcomeStore = new OutcomeStore(userIds);
        List<UserData> resolvedUsers = userIds.stream()
                .map(userId -> DataManager.getInstance().findUser(userId, DataManager.getDefaultUser()))
                .toList();
        this.participantsStore = new ParticipantsStore(resolvedUsers);
    }

    public UUID getId() {
        return id.asUuid();
    }

    public GameSessionId sessionId() {
        return id;
    }

    public List<UserData> getUsers() {
        return participantsStore.participants();
    }

    public List<String> getUserIds() {
        return participantsStore.participantIds();
    }

    public long getCreatedAt() {
        return lifecycleStore.createdAt();
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
        return rulesStore.rulesContext();
    }

    public void setRulesContext(RulesContext rulesContext) {
        rulesStore.rulesContext(rulesContext);
    }

    public void setLowestParticipantScore(int score) {
        rulesStore.lowestParticipantScore(score);
    }

    public void setDesiredRuleCount(int count) {
        rulesStore.desiredRuleCount(count);
    }

    public boolean isGameStarted() {
        return lifecycleStore.gameStarted();
    }

    public long getGameStartedAt() {
        return lifecycleStore.gameStartedAt();
    }

    public void markGameStarted(long startedAt) {
        lifecycleStore.markGameStarted(startedAt);
    }

    public boolean isReady(String userId) {
        return participantsStore.isReady(userId);
    }

    public boolean markReady(String userId) {
        return participantsStore.markReady(userId);
    }

    public boolean allReady() {
        return participantsStore.allReady();
    }

    public boolean markDisconnected(String userId) {
        return participantsStore.markDisconnected(userId);
    }

    public void clearDisconnected(String userId) {
        participantsStore.clearDisconnected(userId);
    }

    public Set<String> disconnectedUsersView() {
        return participantsStore.disconnectedView();
    }

    public boolean recordPostGameDecision(String userId, PostGameDecision decision) {
        if (userId == null || decision == null || !containsUser(userId)) {
            return false;
        }
        boolean recorded = postGameStore.recordDecision(userId, decision);
        if (!recorded) {
            return false;
        }
        if (decision == PostGameDecision.REMATCH) {
            clearDisconnected(userId);
        } else {
            markDisconnected(userId);
        }
        return true;
    }

    public boolean hasPostGameDecision(String userId) {
        return postGameStore.hasDecision(userId);
    }

    public Map<String, PostGameDecision> postGameDecisionsView() {
        return postGameStore.decisionsView();
    }

    public Set<String> rematchRequestsView() {
        return postGameStore.rematchRequestsView();
    }

    public void resetPostGameDecisions() {
        postGameStore.reset();
    }

    public int playerIndexOf(String userId) {
        return participantsStore.indexOf(userId);
    }

    public boolean containsUser(String userId) {
        return participantsStore.contains(userId);
    }

    public void resetOutcomes() {
        outcomeStore.reset(getUserIds());
    }

    public PlayerResult outcomeFor(String userId) {
        return outcomeStore.resultFor(userId);
    }

    public void updateOutcome(String userId, PlayerResult result) {
        outcomeStore.updateResult(userId, result);
    }

    public boolean isGameFinished() {
        return outcomeStore.isResolved();
    }

    public long getGameEndedAt() {
        return lifecycleStore.gameEndedAt();
    }

    public void markGameFinished(long endedAt, int turnCount) {
        lifecycleStore.markGameFinished(endedAt, turnCount);
    }

    public int getCompletedTurnCount() {
        int recorded = lifecycleStore.completedTurnCount();
        return recorded > 0 ? recorded : turnStore.actionNumber();
    }

    public long getGameDurationMillis() {
        return lifecycleStore.gameDurationMillis();
    }
}
