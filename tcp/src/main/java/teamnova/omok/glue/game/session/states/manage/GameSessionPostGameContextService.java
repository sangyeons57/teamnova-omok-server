package teamnova.omok.glue.game.session.states.manage;

import java.util.Objects;

import teamnova.omok.glue.game.session.model.messages.BoardSnapshotUpdate;
import teamnova.omok.glue.game.session.model.messages.GameCompletionNotice;
import teamnova.omok.glue.game.session.model.messages.PostGameDecisionPrompt;
import teamnova.omok.glue.game.session.model.messages.PostGameDecisionUpdate;
import teamnova.omok.glue.game.session.model.messages.PostGameResolution;

/**
 * Stateless operations for managing post-game buffers within {@link GameSessionStateContext}.
 */
public final class GameSessionPostGameContextService {

    public void queueDecisionUpdate(GameSessionStateContext context, PostGameDecisionUpdate update) {
        Objects.requireNonNull(context, "context");
        context.postGameRuntime().setPendingDecisionUpdate(update);
    }

    public PostGameDecisionUpdate consumeDecisionUpdate(GameSessionStateContext context) {
        Objects.requireNonNull(context, "context");
        PostGameDecisionUpdate update = context.postGameRuntime().getPendingDecisionUpdate();
        context.postGameRuntime().clearPendingDecisionUpdate();
        return update;
    }

    public void queueDecisionPrompt(GameSessionStateContext context, PostGameDecisionPrompt prompt) {
        Objects.requireNonNull(context, "context");
        context.postGameRuntime().setPendingDecisionPrompt(prompt);
    }

    public PostGameDecisionPrompt consumeDecisionPrompt(GameSessionStateContext context) {
        Objects.requireNonNull(context, "context");
        PostGameDecisionPrompt prompt = context.postGameRuntime().getPendingDecisionPrompt();
        context.postGameRuntime().clearPendingDecisionPrompt();
        return prompt;
    }

    public void queuePostGameResolution(GameSessionStateContext context, PostGameResolution resolution) {
        Objects.requireNonNull(context, "context");
        context.postGameRuntime().setPendingPostGameResolution(resolution);
    }

    public PostGameResolution consumePostGameResolution(GameSessionStateContext context) {
        Objects.requireNonNull(context, "context");
        PostGameResolution resolution = context.postGameRuntime().getPendingPostGameResolution();
        context.postGameRuntime().clearPendingPostGameResolution();
        return resolution;
    }

    public void setDecisionDeadline(GameSessionStateContext context, long deadline) {
        Objects.requireNonNull(context, "context");
        context.postGameRuntime().setPostGameDecisionDeadline(deadline);
    }

    public long decisionDeadline(GameSessionStateContext context) {
        Objects.requireNonNull(context, "context");
        return context.postGameRuntime().getPostGameDecisionDeadline();
    }

    public void clearDecisionDeadline(GameSessionStateContext context) {
        Objects.requireNonNull(context, "context");
        context.postGameRuntime().clearPostGameDecisionDeadline();
    }

    public void queueGameCompletion(GameSessionStateContext context, GameCompletionNotice notice) {
        Objects.requireNonNull(context, "context");
        context.postGameRuntime().setPendingGameCompletion(notice);
    }

    public GameCompletionNotice consumeGameCompletion(GameSessionStateContext context) {
        Objects.requireNonNull(context, "context");
        GameCompletionNotice notice = context.postGameRuntime().getPendingGameCompletion();
        context.postGameRuntime().clearPendingGameCompletion();
        return notice;
    }

    public void queueBoardSnapshot(GameSessionStateContext context, BoardSnapshotUpdate update) {
        Objects.requireNonNull(context, "context");
        context.postGameRuntime().setPendingBoardSnapshot(update);
    }

    public BoardSnapshotUpdate consumeBoardSnapshot(GameSessionStateContext context) {
        Objects.requireNonNull(context, "context");
        BoardSnapshotUpdate update = context.postGameRuntime().getPendingBoardSnapshot();
        context.postGameRuntime().clearPendingBoardSnapshot();
        return update;
    }
}
