package teamnova.omok.state.game.manage;

import java.util.Objects;

import teamnova.omok.service.BoardService;
import teamnova.omok.service.InGameSessionService;
import teamnova.omok.service.OutcomeService;
import teamnova.omok.service.TurnService;
import teamnova.omok.store.GameSession;

/**
 * Shared context passed to game session state implementations.
 */
public final class GameSessionStateContext {
    private final GameSession session;
    private final BoardService boardService;
    private final TurnService turnService;
    private final OutcomeService outcomeService;

    private TurnCycleContext activeTurnCycle;
    private InGameSessionService.MoveResult pendingMoveResult;
    private InGameSessionService.ReadyResult pendingReadyResult;
    private InGameSessionService.TurnTimeoutResult pendingTimeoutResult;
    private InGameSessionService.PostGameDecisionResult pendingDecisionResult;
    private InGameSessionService.PostGameDecisionUpdate pendingDecisionUpdate;
    private InGameSessionService.PostGameDecisionPrompt pendingDecisionPrompt;
    private InGameSessionService.PostGameResolution pendingPostGameResolution;
    private long postGameDecisionDeadline;
    private InGameSessionService.GameCompletionNotice pendingGameCompletion;

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

    public void pendingMoveResult(InGameSessionService.MoveResult result) {
        this.pendingMoveResult = result;
    }

    public InGameSessionService.MoveResult consumePendingMoveResult() {
        InGameSessionService.MoveResult result = this.pendingMoveResult;
        this.pendingMoveResult = null;
        return result;
    }

    public void pendingReadyResult(InGameSessionService.ReadyResult result) {
        this.pendingReadyResult = result;
    }

    public InGameSessionService.ReadyResult consumePendingReadyResult() {
        InGameSessionService.ReadyResult result = this.pendingReadyResult;
        this.pendingReadyResult = null;
        return result;
    }

    public void pendingTimeoutResult(InGameSessionService.TurnTimeoutResult result) {
        this.pendingTimeoutResult = result;
    }

    public InGameSessionService.TurnTimeoutResult consumePendingTimeoutResult() {
        InGameSessionService.TurnTimeoutResult result = this.pendingTimeoutResult;
        this.pendingTimeoutResult = null;
        return result;
    }

    public void pendingDecisionResult(InGameSessionService.PostGameDecisionResult result) {
        this.pendingDecisionResult = result;
    }

    public InGameSessionService.PostGameDecisionResult consumePendingDecisionResult() {
        InGameSessionService.PostGameDecisionResult result = this.pendingDecisionResult;
        this.pendingDecisionResult = null;
        return result;
    }

    public void pendingDecisionUpdate(InGameSessionService.PostGameDecisionUpdate update) {
        this.pendingDecisionUpdate = update;
    }

    public InGameSessionService.PostGameDecisionUpdate consumePendingDecisionUpdate() {
        InGameSessionService.PostGameDecisionUpdate update = this.pendingDecisionUpdate;
        this.pendingDecisionUpdate = null;
        return update;
    }

    public void pendingDecisionPrompt(InGameSessionService.PostGameDecisionPrompt prompt) {
        this.pendingDecisionPrompt = prompt;
    }

    public InGameSessionService.PostGameDecisionPrompt consumePendingDecisionPrompt() {
        InGameSessionService.PostGameDecisionPrompt prompt = this.pendingDecisionPrompt;
        this.pendingDecisionPrompt = null;
        return prompt;
    }

    public void pendingPostGameResolution(InGameSessionService.PostGameResolution resolution) {
        this.pendingPostGameResolution = resolution;
    }

    public InGameSessionService.PostGameResolution consumePendingPostGameResolution() {
        InGameSessionService.PostGameResolution resolution = this.pendingPostGameResolution;
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

    public void pendingGameCompletion(InGameSessionService.GameCompletionNotice notice) {
        this.pendingGameCompletion = notice;
    }

    public InGameSessionService.GameCompletionNotice consumePendingGameCompletion() {
        InGameSessionService.GameCompletionNotice notice = this.pendingGameCompletion;
        this.pendingGameCompletion = null;
        return notice;
    }
}
