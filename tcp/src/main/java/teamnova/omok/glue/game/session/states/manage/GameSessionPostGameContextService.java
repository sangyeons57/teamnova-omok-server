package teamnova.omok.glue.game.session.states.manage;

import java.util.Objects;

import teamnova.omok.glue.game.session.model.messages.BoardSnapshotUpdate;
import teamnova.omok.glue.game.session.model.messages.GameCompletionNotice;
import teamnova.omok.glue.game.session.model.messages.PostGameDecisionPrompt;
import teamnova.omok.glue.game.session.model.messages.PostGameDecisionUpdate;
import teamnova.omok.glue.game.session.model.messages.PostGameResolution;
import teamnova.omok.glue.game.session.model.result.PostGameDecisionResult;

/**
 * Stateless operations for managing post-game buffers within {@link GameSessionStateContext}.
 */
public final class GameSessionPostGameContextService {

    public void queueDecisionResult(GameSessionStateContext context, PostGameDecisionResult result) {
        Objects.requireNonNull(context, "context");
        context.setPendingDecisionResult(result);
    }

    public PostGameDecisionResult consumeDecisionResult(GameSessionStateContext context) {
        Objects.requireNonNull(context, "context");
        PostGameDecisionResult result = context.getPendingDecisionResult();
        context.clearPendingDecisionResult();
        return result;
    }

    public void queueDecisionUpdate(GameSessionStateContext context, PostGameDecisionUpdate update) {
        Objects.requireNonNull(context, "context");
        context.setPendingDecisionUpdate(update);
    }

    public PostGameDecisionUpdate consumeDecisionUpdate(GameSessionStateContext context) {
        Objects.requireNonNull(context, "context");
        PostGameDecisionUpdate update = context.getPendingDecisionUpdate();
        context.clearPendingDecisionUpdate();
        return update;
    }

    public void queueDecisionPrompt(GameSessionStateContext context, PostGameDecisionPrompt prompt) {
        Objects.requireNonNull(context, "context");
        context.setPendingDecisionPrompt(prompt);
    }

    public PostGameDecisionPrompt consumeDecisionPrompt(GameSessionStateContext context) {
        Objects.requireNonNull(context, "context");
        PostGameDecisionPrompt prompt = context.getPendingDecisionPrompt();
        context.clearPendingDecisionPrompt();
        return prompt;
    }

    public void queuePostGameResolution(GameSessionStateContext context, PostGameResolution resolution) {
        Objects.requireNonNull(context, "context");
        context.setPendingPostGameResolution(resolution);
    }

    public PostGameResolution consumePostGameResolution(GameSessionStateContext context) {
        Objects.requireNonNull(context, "context");
        PostGameResolution resolution = context.getPendingPostGameResolution();
        context.clearPendingPostGameResolution();
        return resolution;
    }

    public void setDecisionDeadline(GameSessionStateContext context, long deadline) {
        Objects.requireNonNull(context, "context");
        context.setPostGameDecisionDeadline(deadline);
    }

    public long decisionDeadline(GameSessionStateContext context) {
        Objects.requireNonNull(context, "context");
        return context.getPostGameDecisionDeadline();
    }

    public void clearDecisionDeadline(GameSessionStateContext context) {
        Objects.requireNonNull(context, "context");
        context.clearPostGameDecisionDeadline();
    }

    public void queueGameCompletion(GameSessionStateContext context, GameCompletionNotice notice) {
        Objects.requireNonNull(context, "context");
        context.setPendingGameCompletion(notice);
    }

    public GameCompletionNotice consumeGameCompletion(GameSessionStateContext context) {
        Objects.requireNonNull(context, "context");
        GameCompletionNotice notice = context.getPendingGameCompletion();
        context.clearPendingGameCompletion();
        return notice;
    }

    public void queueBoardSnapshot(GameSessionStateContext context, BoardSnapshotUpdate update) {
        Objects.requireNonNull(context, "context");
        context.setPendingBoardSnapshot(update);
    }

    public BoardSnapshotUpdate consumeBoardSnapshot(GameSessionStateContext context) {
        Objects.requireNonNull(context, "context");
        BoardSnapshotUpdate update = context.getPendingBoardSnapshot();
        context.clearPendingBoardSnapshot();
        return update;
    }
}
