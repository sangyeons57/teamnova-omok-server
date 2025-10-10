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
}
