package teamnova.omok.glue.state.game.manage;

import java.util.Objects;

import teamnova.omok.glue.rule.RulesContext;
import teamnova.omok.glue.service.BoardService;
import teamnova.omok.glue.service.OutcomeService;
import teamnova.omok.glue.service.TurnService;
import teamnova.omok.glue.store.GameSession;

import teamnova.omok.glue.service.dto.GameCompletionNotice;
import teamnova.omok.glue.service.dto.MoveResult;
import teamnova.omok.glue.service.dto.BoardSnapshotUpdate;
import teamnova.omok.glue.service.dto.PostGameDecisionPrompt;
import teamnova.omok.glue.service.dto.PostGameDecisionResult;
import teamnova.omok.glue.service.dto.PostGameDecisionUpdate;
import teamnova.omok.glue.service.dto.PostGameResolution;
import teamnova.omok.glue.service.dto.ReadyResult;
import teamnova.omok.glue.service.dto.TurnTimeoutResult;
import teamnova.omok.modules.state_machine.interfaces.StateContext;

/**
 * Shared context passed to game session state implementations.
 */
public final class GameSessionStateContext implements StateContext {
    private final GameSession session;
    private final BoardService boardService;
    private final TurnService turnService;
    private final OutcomeService outcomeService;

    private TurnCycleContext activeTurnCycle;
    private MoveResult pendingMoveResult;
    private ReadyResult pendingReadyResult;
    private TurnTimeoutResult pendingTimeoutResult;
    private PostGameDecisionResult pendingDecisionResult;
    private PostGameDecisionUpdate pendingDecisionUpdate;
    private PostGameDecisionPrompt pendingDecisionPrompt;
    private PostGameResolution pendingPostGameResolution;
    private long postGameDecisionDeadline;
    private GameCompletionNotice pendingGameCompletion;
    private BoardSnapshotUpdate pendingBoardSnapshot;

    public GameSessionStateContext(GameSession session,
                                   BoardService boardService,
                                   TurnService turnService,
                                   OutcomeService outcomeService) {
        this.session = Objects.requireNonNull(session, "session");
        this.boardService = Objects.requireNonNull(boardService, "boardService");
        this.turnService = Objects.requireNonNull(turnService, "turnService");
        this.outcomeService = Objects.requireNonNull(outcomeService, "outcomeService");
    }

    public GameSession session() {
        return session;
    }

    public BoardService boardService() {
        return boardService;
    }

    public TurnService turnService() {
        return turnService;
    }

    public OutcomeService outcomeService() {
        return outcomeService;
    }

    public RulesContext rulesContext() {
        return session.getRulesContext();
    }

    public void beginTurnCycle(TurnCycleContext context) {
        if (this.activeTurnCycle != null) {
            throw new IllegalStateException("Turn cycle already in progress");
        }
        this.activeTurnCycle = context;
        this.pendingMoveResult = null;
    }

    public TurnCycleContext activeTurnCycle() {
        return activeTurnCycle;
    }

    public void clearTurnCycle() {
        this.activeTurnCycle = null;
    }

    public void pendingMoveResult(MoveResult result) {
        this.pendingMoveResult = result;
    }

    public MoveResult consumePendingMoveResult() {
        MoveResult result = this.pendingMoveResult;
        this.pendingMoveResult = null;
        return result;
    }

    public void pendingReadyResult(ReadyResult result) {
        this.pendingReadyResult = result;
    }

    public ReadyResult consumePendingReadyResult() {
        ReadyResult result = this.pendingReadyResult;
        this.pendingReadyResult = null;
        return result;
    }

    public void pendingTimeoutResult(TurnTimeoutResult result) {
        this.pendingTimeoutResult = result;
    }

    public TurnTimeoutResult consumePendingTimeoutResult() {
        TurnTimeoutResult result = this.pendingTimeoutResult;
        this.pendingTimeoutResult = null;
        return result;
    }

    public void pendingDecisionResult(PostGameDecisionResult result) {
        this.pendingDecisionResult = result;
    }

    public PostGameDecisionResult consumePendingDecisionResult() {
        PostGameDecisionResult result = this.pendingDecisionResult;
        this.pendingDecisionResult = null;
        return result;
    }

    public void pendingDecisionUpdate(PostGameDecisionUpdate update) {
        this.pendingDecisionUpdate = update;
    }

    public PostGameDecisionUpdate consumePendingDecisionUpdate() {
        PostGameDecisionUpdate update = this.pendingDecisionUpdate;
        this.pendingDecisionUpdate = null;
        return update;
    }

    public void pendingDecisionPrompt(PostGameDecisionPrompt prompt) {
        this.pendingDecisionPrompt = prompt;
    }

    public PostGameDecisionPrompt consumePendingDecisionPrompt() {
        PostGameDecisionPrompt prompt = this.pendingDecisionPrompt;
        this.pendingDecisionPrompt = null;
        return prompt;
    }

    public void pendingPostGameResolution(PostGameResolution resolution) {
        this.pendingPostGameResolution = resolution;
    }

    public PostGameResolution consumePendingPostGameResolution() {
        PostGameResolution resolution = this.pendingPostGameResolution;
        this.pendingPostGameResolution = null;
        return resolution;
    }

    public void postGameDecisionDeadline(long deadline) {
        this.postGameDecisionDeadline = deadline;
    }

    public long postGameDecisionDeadline() {
        return postGameDecisionDeadline;
    }

    public void clearPostGameDecisionDeadline() {
        this.postGameDecisionDeadline = 0L;
    }

    public void pendingGameCompletion(GameCompletionNotice notice) {
        this.pendingGameCompletion = notice;
    }

    public GameCompletionNotice consumePendingGameCompletion() {
        GameCompletionNotice notice = this.pendingGameCompletion;
        this.pendingGameCompletion = null;
        return notice;
    }

    public void pendingBoardSnapshot(BoardSnapshotUpdate update) {
        this.pendingBoardSnapshot = update;
    }

    public BoardSnapshotUpdate consumePendingBoardSnapshot() {
        BoardSnapshotUpdate update = this.pendingBoardSnapshot;
        this.pendingBoardSnapshot = null;
        return update;
    }
}
