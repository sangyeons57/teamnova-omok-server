package teamnova.omok.domain.session.game;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import teamnova.omok.application.register.GameSessionStateRegister;
import teamnova.omok.domain.session.game.entity.state.GameSessionStateHolder;
import teamnova.omok.domain.session.game.entity.state.GameSessionStateHolderService;
import teamnova.omok.game.PlayerResult;
import teamnova.omok.game.PostGameDecisionType;
import teamnova.omok.domain.rule.RulesContext;
import teamnova.omok.domain.session.game.entity.board.Board;
import teamnova.omok.domain.session.game.entity.board.BoardReadable;
import teamnova.omok.domain.session.game.entity.board.BoardService;
import teamnova.omok.domain.session.game.entity.turn.TurnReadable;
import teamnova.omok.domain.session.game.entity.turn.TurnSnapshot;
import teamnova.omok.domain.session.game.entity.outcome.Outcome;
import teamnova.omok.domain.session.game.entity.stone.Stone;
import teamnova.omok.domain.session.game.entity.turn.Turn;

/**
 * Represents an in-game session with participants and mutable runtime state.
 */
public class GameSession {
    public static final long TURN_DURATION_MILLIS = 15_000L;
    public static final long POST_GAME_DECISION_DURATION_MILLIS = 30_000L;

    private final UUID id;
    private final List<String> userIds;
    private final long createdAt;

    private final ReentrantLock lock = new ReentrantLock();
    private final Map<String, Boolean> readyStates = new ConcurrentHashMap<>();
    // Users who dropped before the game finished or chose to leave during post-game decisions.
    private final Set<String> disconnectedUserIds = ConcurrentHashMap.newKeySet();
    // Tracks players who opted into a rematch during the post-game window.
    private final Board board;
    private final Turn turn;
    private final Outcome outcome;
    private final GameSessionStateHolder stateHolder;
    private final Map<String, PostGameDecisionType> postGameDecisions = new ConcurrentHashMap<>();
    private final Set<String> rematchRequestUserIds = ConcurrentHashMap.newKeySet();
    private volatile RulesContext rulesContext;

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
        this.userIds = List.copyOf(userIds);
        this.createdAt = System.currentTimeMillis();
        this.board = new Board(BoardService.DEFAULT_WIDTH, BoardService.DEFAULT_HEIGHT);
        this.turn = new Turn();
        this.stateHolder = new GameSessionStateHolder();
        this.outcome = new Outcome(this.userIds);
        this.rulesContext = null;

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

    public BoardReadable getBoard() {
        return board.getReadable();
    }

    public void setStone(int x, int y, Stone stone){
        board.setStone(x, y, stone);
    }

    public TurnReadable getTurn(){
        return turn.getReadable();
    }

    public TurnSnapshot startTurn(long now) {
        return turn.start(getUserIds(), now);
    }

    public TurnSnapshot snapshotTurn(){
        return turn.snapshot(getUserIds());
    }

    public TurnSnapshot advanceSkippingDisconnected(long now) {
        return turn.advanceSkippingDisconnected(getUserIds(), disconnectedUserIds, now);
    }

    public GameSessionStateHolder getStateHolder() {
        return stateHolder;
    }


    public void reset(){
        board.clear();

    }

    public Turn getTurnStore() {
        return turn;
    }

    public RulesContext getRulesContext() {
        return rulesContext;
    }

    public void setRulesContext(RulesContext rulesContext) {
        this.rulesContext = rulesContext;
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

    public boolean recordPostGameDecision(String userId, PostGameDecisionType decision) {
        if (userId == null || decision == null || !containsUser(userId)) {
            return false;
        }
        PostGameDecisionType previous = postGameDecisions.putIfAbsent(userId, decision);
        if (previous != null) {
            return false;
        }
        if (decision == PostGameDecisionType.REMATCH) {
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

    public PostGameDecisionType decisionFor(String userId) {
        return postGameDecisions.get(userId);
    }

    public Map<String, PostGameDecisionType> postGameDecisionsView() {
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
        return userIds.indexOf(userId);
    }

    public boolean containsUser(String userId) {
        return readyStates.containsKey(userId);
    }

    public void resetOutcomes() {
        outcome.reset(userIds);
    }

    public PlayerResult outcomeFor(String userId) {
        return outcome.resultFor(userId);
    }

    public boolean updateOutcome(String userId, PlayerResult result) {
        return outcome.updateResult(userId, result);
    }

    public boolean isGameFinished() {
        return outcome.isResolved();
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
        return completedTurnCount > 0 ? completedTurnCount : turn.getTurnNumber();
    }

    public long getGameDurationMillis() {
        long start = gameStartedAt;
        long end = gameEndedAt;
        if (start <= 0 || end <= 0 || end < start) {
            return 0L;
        }
        return end - start;
    }

    public void update(GameSessionStateHolderService holderService, GameSessionStateRegister stateRegister, long now) {
        holderService.process(stateHolder, stateRegister, now);
    }
}
