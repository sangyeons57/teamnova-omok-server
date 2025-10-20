package teamnova.omok.glue.game.session.model;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import teamnova.omok.glue.data.model.UserData;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionAccess;
import teamnova.omok.glue.game.session.model.messages.BoardSnapshotUpdate;
import teamnova.omok.glue.game.session.model.messages.GameCompletionNotice;
import teamnova.omok.glue.game.session.model.PostGameDecision;
import teamnova.omok.glue.game.session.model.messages.PostGameDecisionPrompt;
import teamnova.omok.glue.game.session.model.messages.PostGameDecisionUpdate;
import teamnova.omok.glue.game.session.model.messages.PostGameResolution;
import teamnova.omok.glue.game.session.model.result.MoveResult;
import teamnova.omok.glue.game.session.model.result.PostGameDecisionResult;
import teamnova.omok.glue.game.session.model.result.ReadyResult;
import teamnova.omok.glue.game.session.model.result.TurnTimeoutResult;
import teamnova.omok.glue.game.session.model.runtime.TurnTransition;
import teamnova.omok.glue.game.session.model.store.BoardStore;
import teamnova.omok.glue.game.session.model.store.LifecycleStore;
import teamnova.omok.glue.game.session.model.store.OutcomeStore;
import teamnova.omok.glue.game.session.model.store.ParticipantsStore;
import teamnova.omok.glue.game.session.model.store.PostGameRuntimeStore;
import teamnova.omok.glue.game.session.model.store.PostGameStore;
import teamnova.omok.glue.game.session.model.store.RulesStore;
import teamnova.omok.glue.game.session.model.store.TurnPlacementStore;
import teamnova.omok.glue.game.session.model.store.TurnRuntimeStore;
import teamnova.omok.glue.game.session.model.store.TurnStore;
import teamnova.omok.glue.game.session.model.vo.GameSessionId;
import teamnova.omok.glue.game.session.model.vo.StonePlacementMetadata;
import teamnova.omok.glue.game.session.model.vo.TurnCounters;
import teamnova.omok.glue.game.session.model.vo.TurnOrder;
import teamnova.omok.glue.game.session.model.vo.TurnTiming;
import teamnova.omok.glue.game.session.states.manage.TurnCycleContext;
import teamnova.omok.glue.manager.DataManager;
import teamnova.omok.glue.rule.RulesContext;

/**
 * Represents an in-game session with participants and mutable runtime state.
 */
public class GameSession implements GameSessionAccess {
    public static final int BOARD_WIDTH = BoardStore.DEFAULT_WIDTH;
    public static final int BOARD_HEIGHT = BoardStore.DEFAULT_HEIGHT;
    public static final long TURN_DURATION_MILLIS = 15_000L;
    public static final long POST_GAME_DECISION_DURATION_MILLIS = 30_000L;

    private final GameSessionId id;
    private final ParticipantsStore participantsStore;
    private final LifecycleStore lifecycleStore;
    private final RulesStore rulesStore = new RulesStore();
    private final BoardStore boardStore;
    private final TurnPlacementStore placementStore;
    private final TurnStore turnStore;
    private final OutcomeStore outcomeStore;
    private final PostGameStore postGameStore = new PostGameStore();
    private final TurnRuntimeStore turnRuntimeStore = new TurnRuntimeStore();
    private final PostGameRuntimeStore postGameRuntimeStore = new PostGameRuntimeStore();
    private final ReentrantLock lock = new ReentrantLock();

    public GameSession(List<String> userIds) {
        Objects.requireNonNull(userIds, "userIds");
        if (userIds.isEmpty()) {
            throw new IllegalArgumentException("userIds must not be empty");
        }
        this.id = GameSessionId.random();
        this.lifecycleStore = new LifecycleStore(System.currentTimeMillis());
        this.boardStore = new BoardStore(BOARD_WIDTH, BOARD_HEIGHT);
        this.placementStore = new TurnPlacementStore(BOARD_WIDTH, BOARD_HEIGHT);
        this.turnStore = new TurnStore();
        this.outcomeStore = new OutcomeStore(userIds);
        List<UserData> resolvedUsers = userIds.stream()
                .map(userId -> DataManager.getInstance().findUser(userId, DataManager.getDefaultUser()))
                .toList();
        this.participantsStore = new ParticipantsStore(resolvedUsers);
    }

    public GameSessionId sessionId() {
        return id;
    }

    public ReentrantLock lock() {
        return lock;
    }

    @Override
    public RulesContext getRulesContext() {
        return rulesStore.rulesContext();
    }

    @Override
    public void setRulesContext(RulesContext rulesContext) {
        rulesStore.rulesContext(rulesContext);
    }

    @Override
    public void setLowestParticipantScore(int score) {
        rulesStore.lowestParticipantScore(score);
    }

    @Override
    public void setDesiredRuleCount(int count) {
        rulesStore.desiredRuleCount(count);
    }

    @Override
    public long getCreatedAt() {
        return lifecycleStore.createdAt();
    }

    @Override
    public boolean isGameStarted() {
        return lifecycleStore.gameStarted();
    }

    @Override
    public long getGameStartedAt() {
        return lifecycleStore.gameStartedAt();
    }

    @Override
    public void markGameStarted(long startedAt) {
        lifecycleStore.markGameStarted(startedAt);
    }

    @Override
    public long getGameEndedAt() {
        return lifecycleStore.gameEndedAt();
    }

    @Override
    public void markGameFinished(long endedAt, int turnCount) {
        lifecycleStore.markGameFinished(endedAt, turnCount);
    }

    @Override
    public int getCompletedTurnCount() {
        int recorded = lifecycleStore.completedTurnCount();
        return recorded > 0 ? recorded : turnStore.actionNumber();
    }

    @Override
    public long getGameDurationMillis() {
        return lifecycleStore.gameDurationMillis();
    }

    @Override
    public List<UserData> getUsers() {
        return participantsStore.participants();
    }

    @Override
    public List<String> getUserIds() {
        return participantsStore.participantIds();
    }

    @Override
    public boolean isReady(String userId) {
        return participantsStore.isReady(userId);
    }

    @Override
    public boolean markReady(String userId) {
        return participantsStore.markReady(userId);
    }

    @Override
    public boolean allReady() {
        return participantsStore.allReady();
    }

    @Override
    public boolean markDisconnected(String userId) {
        return participantsStore.markDisconnected(userId);
    }

    @Override
    public void clearDisconnected(String userId) {
        participantsStore.clearDisconnected(userId);
    }

    @Override
    public Set<String> disconnectedUsersView() {
        return participantsStore.disconnectedView();
    }

    @Override
    public int playerIndexOf(String userId) {
        return participantsStore.indexOf(userId);
    }

    @Override
    public boolean containsUser(String userId) {
        return participantsStore.contains(userId);
    }

    @Override
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

    @Override
    public boolean hasPostGameDecision(String userId) {
        return postGameStore.hasDecision(userId);
    }

    @Override
    public Map<String, PostGameDecision> postGameDecisionsView() {
        return postGameStore.decisionsView();
    }

    @Override
    public Set<String> rematchRequestsView() {
        return postGameStore.rematchRequestsView();
    }

    @Override
    public void resetPostGameDecisions() {
        postGameStore.reset();
    }



    @Override
    public void resetOutcomes() {
        outcomeStore.reset(getUserIds());
    }

    @Override
    public PlayerResult outcomeFor(String userId) {
        return outcomeStore.resultFor(userId);
    }

    @Override
    public void updateOutcome(String userId, PlayerResult result) {
        outcomeStore.updateResult(userId, result);
    }

    @Override
    public boolean isGameFinished() {
        return outcomeStore.isResolved();
    }

    @Override
    public int width() {
        return boardStore.width();
    }

    @Override
    public int height() {
        return boardStore.height();
    }

    @Override
    public boolean isWithinBounds(int x, int y) {
        return x >= 0 && x < boardStore.width() && y >= 0 && y < boardStore.height();
    }

    @Override
    public boolean isEmpty(int x, int y) {
        return Stone.fromByte(boardStore.get(linearIndex(x, y))) == Stone.EMPTY;
    }

    @Override
    public Stone stoneAt(int x, int y) {
        return Stone.fromByte(boardStore.get(linearIndex(x, y)));
    }

    @Override
    public void setStone(int x, int y, Stone stone) {
        boardStore.set(linearIndex(x, y), stone.code());
    }

    @Override
    public StonePlacementMetadata placementAt(int x, int y) {
        return placementStore.get(linearIndex(x, y));
    }

    @Override
    public void recordPlacement(int x, int y, StonePlacementMetadata metadata) {
        placementStore.record(linearIndex(x, y), metadata);
    }

    @Override
    public void clearPlacement(int x, int y) {
        placementStore.clear(linearIndex(x, y));
    }

    @Override
    public void clearPlacements() {
        placementStore.reset();
    }

    private int linearIndex(int x, int y) {
        return y * boardStore.width() + x;
    }

    @Override
    public void clear() {
        boardStore.clear();
        placementStore.reset();
    }

    @Override
    public byte[] snapshot() {
        return boardStore.snapshot();
    }

    @Override
    public TurnOrder order() {
        return turnStore.order();
    }

    @Override
    public void order(TurnOrder order) {
        turnStore.order(order);
    }

    @Override
    public TurnCounters counters() {
        return turnStore.counters();
    }

    @Override
    public void counters(TurnCounters counters) {
        turnStore.counters(counters);
    }

    @Override
    public int actionNumber() {
        return turnStore.actionNumber();
    }

    @Override
    public int getCurrentPlayerIndex() {
        return turnStore.getCurrentPlayerIndex();
    }

    @Override
    public void setCurrentPlayerIndex(int currentPlayerIndex) {
        turnStore.setCurrentPlayerIndex(currentPlayerIndex);
    }

    @Override
    public TurnTiming timing() {
        return turnStore.timing();
    }

    @Override
    public void timing(TurnTiming timing) {
        turnStore.timing(timing);
    }

    @Override
    public void reset() {
        turnStore.reset();
    }

    @Override
    public TurnCycleContext getActiveTurnCycle() {
        return turnRuntimeStore.getActiveTurnCycle();
    }

    @Override
    public void setActiveTurnCycle(TurnCycleContext context) {
        turnRuntimeStore.setActiveTurnCycle(context);
    }

    @Override
    public void clearActiveTurnCycle() {
        turnRuntimeStore.clearActiveTurnCycle();
    }

    @Override
    public MoveResult getPendingMoveResult() {
        return turnRuntimeStore.getPendingMoveResult();
    }

    @Override
    public void setPendingMoveResult(MoveResult result) {
        turnRuntimeStore.setPendingMoveResult(result);
    }

    @Override
    public void clearPendingMoveResult() {
        turnRuntimeStore.clearPendingMoveResult();
    }

    @Override
    public ReadyResult getPendingReadyResult() {
        return turnRuntimeStore.getPendingReadyResult();
    }

    @Override
    public void setPendingReadyResult(ReadyResult result) {
        turnRuntimeStore.setPendingReadyResult(result);
    }

    @Override
    public void clearPendingReadyResult() {
        turnRuntimeStore.clearPendingReadyResult();
    }

    @Override
    public TurnTimeoutResult getPendingTimeoutResult() {
        return turnRuntimeStore.getPendingTimeoutResult();
    }

    @Override
    public void setPendingTimeoutResult(TurnTimeoutResult result) {
        turnRuntimeStore.setPendingTimeoutResult(result);
    }

    @Override
    public void clearPendingTimeoutResult() {
        turnRuntimeStore.clearPendingTimeoutResult();
    }

    @Override
    public TurnTransition getPendingTurnTransition() {
        return turnRuntimeStore.getPendingTurnTransition();
    }

    @Override
    public void setPendingTurnTransition(TurnTransition transition) {
        turnRuntimeStore.setPendingTurnTransition(transition);
    }

    @Override
    public void clearPendingTurnTransition() {
        turnRuntimeStore.clearPendingTurnTransition();
    }

    @Override
    public PostGameDecisionResult getPendingDecisionResult() {
        return postGameRuntimeStore.getPendingDecisionResult();
    }

    @Override
    public void setPendingDecisionResult(PostGameDecisionResult result) {
        postGameRuntimeStore.setPendingDecisionResult(result);
    }

    @Override
    public void clearPendingDecisionResult() {
        postGameRuntimeStore.clearPendingDecisionResult();
    }

    @Override
    public PostGameDecisionUpdate getPendingDecisionUpdate() {
        return postGameRuntimeStore.getPendingDecisionUpdate();
    }

    @Override
    public void setPendingDecisionUpdate(PostGameDecisionUpdate update) {
        postGameRuntimeStore.setPendingDecisionUpdate(update);
    }

    @Override
    public void clearPendingDecisionUpdate() {
        postGameRuntimeStore.clearPendingDecisionUpdate();
    }

    @Override
    public PostGameDecisionPrompt getPendingDecisionPrompt() {
        return postGameRuntimeStore.getPendingDecisionPrompt();
    }

    @Override
    public void setPendingDecisionPrompt(PostGameDecisionPrompt prompt) {
        postGameRuntimeStore.setPendingDecisionPrompt(prompt);
    }

    @Override
    public void clearPendingDecisionPrompt() {
        postGameRuntimeStore.clearPendingDecisionPrompt();
    }

    @Override
    public PostGameResolution getPendingPostGameResolution() {
        return postGameRuntimeStore.getPendingPostGameResolution();
    }

    @Override
    public void setPendingPostGameResolution(PostGameResolution resolution) {
        postGameRuntimeStore.setPendingPostGameResolution(resolution);
    }

    @Override
    public void clearPendingPostGameResolution() {
        postGameRuntimeStore.clearPendingPostGameResolution();
    }

    @Override
    public long getPostGameDecisionDeadline() {
        return postGameRuntimeStore.getPostGameDecisionDeadline();
    }

    @Override
    public void setPostGameDecisionDeadline(long deadline) {
        postGameRuntimeStore.setPostGameDecisionDeadline(deadline);
    }

    @Override
    public void clearPostGameDecisionDeadline() {
        postGameRuntimeStore.clearPostGameDecisionDeadline();
    }

    @Override
    public GameCompletionNotice getPendingGameCompletion() {
        return postGameRuntimeStore.getPendingGameCompletion();
    }

    @Override
    public void setPendingGameCompletion(GameCompletionNotice notice) {
        postGameRuntimeStore.setPendingGameCompletion(notice);
    }

    @Override
    public void clearPendingGameCompletion() {
        postGameRuntimeStore.clearPendingGameCompletion();
    }

    @Override
    public BoardSnapshotUpdate getPendingBoardSnapshot() {
        return postGameRuntimeStore.getPendingBoardSnapshot();
    }

    @Override
    public void setPendingBoardSnapshot(BoardSnapshotUpdate update) {
        postGameRuntimeStore.setPendingBoardSnapshot(update);
    }

    @Override
    public void clearPendingBoardSnapshot() {
        postGameRuntimeStore.clearPendingBoardSnapshot();
    }
}
