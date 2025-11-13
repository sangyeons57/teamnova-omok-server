package teamnova.omok.glue.game.session.interfaces;

import java.util.List;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionAccess;
import teamnova.omok.glue.game.session.model.dto.TurnSnapshot;
import teamnova.omok.glue.game.session.model.runtime.TurnPersonalFrame;
import teamnova.omok.glue.game.session.model.messages.PostGameDecisionPrompt;
import teamnova.omok.glue.game.session.model.messages.PostGameDecisionUpdate;
import teamnova.omok.glue.game.session.model.result.ReadyResult;

public interface GameSessionMessenger {
    void broadcastJoin(GameSessionAccess session);
    void broadcastReady(GameSessionAccess session, ReadyResult result);
    void broadcastGameStart(GameSessionAccess session, TurnSnapshot turn);
    void broadcastTurnStarted(GameSessionAccess session, TurnSnapshot snapshot);
    void deliverTurnStarted(GameSessionAccess session, TurnSnapshot snapshot, String targetUserId);
    void broadcastTurnEnded(GameSessionAccess session, TurnPersonalFrame frame);
    // Convenience overload: broadcast current board snapshot using only session
    void broadcastBoardSnapshot(GameSessionAccess session);
    void deliverBoardSnapshot(GameSessionAccess session, String targetUserId);
    void broadcastGameCompleted(GameSessionAccess session);
    void broadcastPostGamePrompt(GameSessionAccess session, PostGameDecisionPrompt prompt);
    void broadcastPostGameDecisionUpdate(GameSessionAccess session, PostGameDecisionUpdate update);
    void broadcastSessionTerminated(GameSessionAccess session, List<String> disconnected);
    void broadcastRematchStarted(GameSessionAccess previous, GameSessionAccess rematch, List<String> participants);
    void broadcastPlayerDisconnected(GameSessionAccess session, String userId, String reason);
}
