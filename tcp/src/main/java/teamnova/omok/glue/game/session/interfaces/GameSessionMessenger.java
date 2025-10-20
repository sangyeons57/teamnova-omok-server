package teamnova.omok.glue.game.session.interfaces;

import java.util.List;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionAccess;
import teamnova.omok.glue.handler.register.Type;
import teamnova.omok.glue.game.session.model.messages.BoardSnapshotUpdate;
import teamnova.omok.glue.game.session.model.result.MoveResult;
import teamnova.omok.glue.game.session.model.messages.PostGameDecisionPrompt;
import teamnova.omok.glue.game.session.model.result.PostGameDecisionResult;
import teamnova.omok.glue.game.session.model.messages.PostGameDecisionUpdate;
import teamnova.omok.glue.game.session.model.result.ReadyResult;
import teamnova.omok.glue.game.session.model.result.TurnTimeoutResult;

public interface GameSessionMessenger {
    void broadcastJoin(GameSessionAccess session);
    void broadcastReady(ReadyResult result);
    void broadcastGameStart(GameSessionAccess session, GameTurnService.TurnSnapshot turn);
    void broadcastStonePlaced(MoveResult result);
    void broadcastTurnTimeout(GameSessionAccess session, TurnTimeoutResult result);
    void broadcastBoardSnapshot(BoardSnapshotUpdate update);
    void broadcastGameCompleted(GameSessionAccess session);
    void broadcastPostGamePrompt(PostGameDecisionPrompt prompt);
    void broadcastPostGameDecisionUpdate(PostGameDecisionUpdate update);
    void broadcastSessionTerminated(GameSessionAccess session, List<String> disconnected);
    void broadcastRematchStarted(GameSessionAccess previous, GameSessionAccess rematch, List<String> participants);
    void broadcastPlayerDisconnected(GameSessionAccess session, String userId, String reason);
    void respondReady(String userId, long requestId, ReadyResult result);
    void respondMove(String userId, long requestId, MoveResult result);
    void respondPostGameDecision(String userId, long requestId, PostGameDecisionResult result);
    void respondError(String userId, Type type, long requestId, String message);
}
