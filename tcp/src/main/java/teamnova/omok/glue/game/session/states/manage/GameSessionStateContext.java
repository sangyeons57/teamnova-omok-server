package teamnova.omok.glue.game.session.states.manage;

import java.util.Objects;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionAccess;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionBoardAccess;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionLifecycleAccess;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionOutcomeAccess;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionParticipantsAccess;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionPostGameAccess;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionRuleAccess;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionTurnAccess;
import teamnova.omok.glue.game.session.model.GameSession;
import teamnova.omok.glue.game.session.model.messages.BoardSnapshotUpdate;
import teamnova.omok.glue.game.session.model.messages.GameCompletionNotice;
import teamnova.omok.glue.game.session.model.messages.PostGameDecisionPrompt;
import teamnova.omok.glue.game.session.model.messages.PostGameDecisionUpdate;
import teamnova.omok.glue.game.session.model.messages.PostGameResolution;
import teamnova.omok.glue.game.session.model.result.MoveResult;
import teamnova.omok.glue.game.session.model.result.PostGameDecisionResult;
import teamnova.omok.glue.game.session.model.result.ReadyResult;
import teamnova.omok.glue.game.session.model.result.TurnTimeoutResult;
import teamnova.omok.modules.state_machine.interfaces.StateContext;

/**
 * Shared context passed to game session state implementations. Holds mutable
 * state while delegating behavioral logic to {@link GameSessionStateContextService}.
 */
public final class GameSessionStateContext implements StateContext {
    private final GameSession session;
    private final GameSessionAccess access;

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

    public GameSessionStateContext(GameSession session) {
        this.session = Objects.requireNonNull(session, "session");
        this.access = session;
    }

    public GameSessionAccess session() {
        return session;
    }

    public GameSessionBoardAccess board() {
        return access;
    }

    public GameSessionTurnAccess turns() {
        return access;
    }

    public GameSessionParticipantsAccess participants() {
        return access;
    }

    public GameSessionOutcomeAccess outcomes() {
        return access;
    }

    public GameSessionPostGameAccess postGame() {
        return access;
    }

    public GameSessionLifecycleAccess lifecycle() {
        return access;
    }

    public GameSessionRuleAccess rules() {
        return access;
    }

    public GameSessionAccess view() {
        return access;
    }

    TurnCycleContext getActiveTurnCycle() {
        return activeTurnCycle;
    }

    void setActiveTurnCycle(TurnCycleContext context) {
        this.activeTurnCycle = context;
    }

    void clearActiveTurnCycle() {
        this.activeTurnCycle = null;
    }

    MoveResult getPendingMoveResult() {
        return pendingMoveResult;
    }

    void setPendingMoveResult(MoveResult result) {
        this.pendingMoveResult = result;
    }

    void clearPendingMoveResult() {
        this.pendingMoveResult = null;
    }

    ReadyResult getPendingReadyResult() {
        return pendingReadyResult;
    }

    void setPendingReadyResult(ReadyResult result) {
        this.pendingReadyResult = result;
    }

    void clearPendingReadyResult() {
        this.pendingReadyResult = null;
    }

    TurnTimeoutResult getPendingTimeoutResult() {
        return pendingTimeoutResult;
    }

    void setPendingTimeoutResult(TurnTimeoutResult result) {
        this.pendingTimeoutResult = result;
    }

    void clearPendingTimeoutResult() {
        this.pendingTimeoutResult = null;
    }

    PostGameDecisionResult getPendingDecisionResult() {
        return pendingDecisionResult;
    }

    void setPendingDecisionResult(PostGameDecisionResult result) {
        this.pendingDecisionResult = result;
    }

    void clearPendingDecisionResult() {
        this.pendingDecisionResult = null;
    }

    PostGameDecisionUpdate getPendingDecisionUpdate() {
        return pendingDecisionUpdate;
    }

    void setPendingDecisionUpdate(PostGameDecisionUpdate update) {
        this.pendingDecisionUpdate = update;
    }

    void clearPendingDecisionUpdate() {
        this.pendingDecisionUpdate = null;
    }

    PostGameDecisionPrompt getPendingDecisionPrompt() {
        return pendingDecisionPrompt;
    }

    void setPendingDecisionPrompt(PostGameDecisionPrompt prompt) {
        this.pendingDecisionPrompt = prompt;
    }

    void clearPendingDecisionPrompt() {
        this.pendingDecisionPrompt = null;
    }

    PostGameResolution getPendingPostGameResolution() {
        return pendingPostGameResolution;
    }

    void setPendingPostGameResolution(PostGameResolution resolution) {
        this.pendingPostGameResolution = resolution;
    }

    void clearPendingPostGameResolution() {
        this.pendingPostGameResolution = null;
    }

    long getPostGameDecisionDeadline() {
        return postGameDecisionDeadline;
    }

    void setPostGameDecisionDeadline(long deadline) {
        this.postGameDecisionDeadline = deadline;
    }

    void clearPostGameDecisionDeadline() {
        this.postGameDecisionDeadline = 0L;
    }

    GameCompletionNotice getPendingGameCompletion() {
        return pendingGameCompletion;
    }

    void setPendingGameCompletion(GameCompletionNotice notice) {
        this.pendingGameCompletion = notice;
    }

    void clearPendingGameCompletion() {
        this.pendingGameCompletion = null;
    }

    BoardSnapshotUpdate getPendingBoardSnapshot() {
        return pendingBoardSnapshot;
    }

    void setPendingBoardSnapshot(BoardSnapshotUpdate update) {
        this.pendingBoardSnapshot = update;
    }

    void clearPendingBoardSnapshot() {
        this.pendingBoardSnapshot = null;
    }
}
