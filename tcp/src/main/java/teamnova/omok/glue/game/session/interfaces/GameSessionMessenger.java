package teamnova.omok.glue.game.session.interfaces;

import java.util.List;

import teamnova.omok.glue.game.session.model.GameSession;
import teamnova.omok.glue.handler.register.Type;
import teamnova.omok.glue.service.dto.BoardSnapshotUpdate;
import teamnova.omok.glue.service.dto.MoveResult;
import teamnova.omok.glue.service.dto.PostGameDecisionPrompt;
import teamnova.omok.glue.service.dto.PostGameDecisionResult;
import teamnova.omok.glue.service.dto.PostGameDecisionUpdate;
import teamnova.omok.glue.service.dto.ReadyResult;
import teamnova.omok.glue.service.dto.TurnTimeoutResult;

public interface GameSessionMessenger {
    void broadcastJoin(GameSession session);
    void broadcastReady(ReadyResult result);
    void broadcastGameStart(GameSession session, GameTurnService.TurnSnapshot turn);
    void broadcastStonePlaced(MoveResult result);
    void broadcastTurnTimeout(GameSession session, TurnTimeoutResult result);
    void broadcastBoardSnapshot(BoardSnapshotUpdate update);
    void broadcastGameCompleted(GameSession session);
    void broadcastPostGamePrompt(PostGameDecisionPrompt prompt);
    void broadcastPostGameDecisionUpdate(PostGameDecisionUpdate update);
    void broadcastSessionTerminated(GameSession session, List<String> disconnected);
    void broadcastRematchStarted(GameSession previous, GameSession rematch, List<String> participants);
    void broadcastPlayerDisconnected(GameSession session, String userId, String reason);
    void respondReady(String userId, long requestId, ReadyResult result);
    void respondMove(String userId, long requestId, MoveResult result);
    void respondPostGameDecision(String userId, long requestId, PostGameDecisionResult result);
    void respondError(String userId, Type type, long requestId, String message);
}
