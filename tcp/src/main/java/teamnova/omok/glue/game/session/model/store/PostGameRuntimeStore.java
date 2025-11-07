package teamnova.omok.glue.game.session.model.store;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionPostGameRuntimeAccess;
import teamnova.omok.glue.game.session.model.GameSession;
import teamnova.omok.glue.game.session.model.messages.BoardSnapshotUpdate;
import teamnova.omok.glue.game.session.model.messages.GameCompletionNotice;
import teamnova.omok.glue.game.session.model.messages.PostGameDecisionPrompt;
import teamnova.omok.glue.game.session.model.messages.PostGameDecisionUpdate;
import teamnova.omok.glue.game.session.model.messages.PostGameResolution;
import teamnova.omok.glue.game.session.model.result.PostGameDecisionResult;

/**
 * Holds transient post-game buffers for a {@link teamnova.omok.glue.game.session.model.GameSession}.
 */
public final class PostGameRuntimeStore implements GameSessionPostGameRuntimeAccess {
    private PostGameDecisionResult pendingDecisionResult;
    private PostGameDecisionUpdate pendingDecisionUpdate;
    private PostGameDecisionPrompt pendingDecisionPrompt;
    private PostGameResolution pendingPostGameResolution;
    private long postGameDecisionDeadline;
    private GameCompletionNotice pendingGameCompletion;
    private BoardSnapshotUpdate pendingBoardSnapshot;
    private GameSession pendingRematchSession;

    @Override
    public PostGameDecisionResult getPendingDecisionResult() {
        return pendingDecisionResult;
    }

    @Override
    public void setPendingDecisionResult(PostGameDecisionResult result) {
        this.pendingDecisionResult = result;
    }

    @Override
    public void clearPendingDecisionResult() {
        this.pendingDecisionResult = null;
    }

    @Override
    public PostGameDecisionUpdate getPendingDecisionUpdate() {
        return pendingDecisionUpdate;
    }

    @Override
    public void setPendingDecisionUpdate(PostGameDecisionUpdate update) {
        this.pendingDecisionUpdate = update;
    }

    @Override
    public void clearPendingDecisionUpdate() {
        this.pendingDecisionUpdate = null;
    }

    @Override
    public PostGameDecisionPrompt getPendingDecisionPrompt() {
        return pendingDecisionPrompt;
    }

    @Override
    public void setPendingDecisionPrompt(PostGameDecisionPrompt prompt) {
        this.pendingDecisionPrompt = prompt;
    }

    @Override
    public void clearPendingDecisionPrompt() {
        this.pendingDecisionPrompt = null;
    }

    @Override
    public PostGameResolution getPendingPostGameResolution() {
        return pendingPostGameResolution;
    }

    @Override
    public void setPendingPostGameResolution(PostGameResolution resolution) {
        this.pendingPostGameResolution = resolution;
    }

    @Override
    public void clearPendingPostGameResolution() {
        this.pendingPostGameResolution = null;
    }

    @Override
    public long getPostGameDecisionDeadline() {
        return postGameDecisionDeadline;
    }

    @Override
    public void setPostGameDecisionDeadline(long deadline) {
        this.postGameDecisionDeadline = deadline;
    }

    @Override
    public void clearPostGameDecisionDeadline() {
        this.postGameDecisionDeadline = 0L;
    }

    @Override
    public GameCompletionNotice getPendingGameCompletion() {
        return pendingGameCompletion;
    }

    @Override
    public void setPendingGameCompletion(GameCompletionNotice notice) {
        this.pendingGameCompletion = notice;
    }

    @Override
    public void clearPendingGameCompletion() {
        this.pendingGameCompletion = null;
    }

    @Override
    public BoardSnapshotUpdate getPendingBoardSnapshot() {
        return pendingBoardSnapshot;
    }

    @Override
    public void setPendingBoardSnapshot(BoardSnapshotUpdate update) {
        this.pendingBoardSnapshot = update;
    }

    @Override
    public void clearPendingBoardSnapshot() {
        this.pendingBoardSnapshot = null;
    }

    @Override
    public GameSession getPendingRematchSession() {
        return pendingRematchSession;
    }

    @Override
    public void setPendingRematchSession(GameSession session) {
        this.pendingRematchSession = session;
    }

    @Override
    public void clearPendingRematchSession() {
        this.pendingRematchSession = null;
    }
}
