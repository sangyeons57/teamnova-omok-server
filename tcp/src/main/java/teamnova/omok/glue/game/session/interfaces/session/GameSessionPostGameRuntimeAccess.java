package teamnova.omok.glue.game.session.interfaces.session;

import teamnova.omok.glue.game.session.model.messages.BoardSnapshotUpdate;
import teamnova.omok.glue.game.session.model.messages.GameCompletionNotice;
import teamnova.omok.glue.game.session.model.messages.PostGameDecisionPrompt;
import teamnova.omok.glue.game.session.model.messages.PostGameDecisionUpdate;
import teamnova.omok.glue.game.session.model.messages.PostGameResolution;

public interface GameSessionPostGameRuntimeAccess {
    PostGameDecisionUpdate getPendingDecisionUpdate();

    void setPendingDecisionUpdate(PostGameDecisionUpdate update);

    void clearPendingDecisionUpdate();

    PostGameDecisionPrompt getPendingDecisionPrompt();

    void setPendingDecisionPrompt(PostGameDecisionPrompt prompt);

    void clearPendingDecisionPrompt();

    PostGameResolution getPendingPostGameResolution();

    void setPendingPostGameResolution(PostGameResolution resolution);

    void clearPendingPostGameResolution();

    long getPostGameDecisionDeadline();

    void setPostGameDecisionDeadline(long deadline);

    void clearPostGameDecisionDeadline();

    GameCompletionNotice getPendingGameCompletion();

    void setPendingGameCompletion(GameCompletionNotice notice);

    void clearPendingGameCompletion();

    BoardSnapshotUpdate getPendingBoardSnapshot();

    void setPendingBoardSnapshot(BoardSnapshotUpdate update);

    void clearPendingBoardSnapshot();
}
