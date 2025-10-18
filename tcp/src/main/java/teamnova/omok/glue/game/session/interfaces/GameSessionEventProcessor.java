package teamnova.omok.glue.game.session.interfaces;

import teamnova.omok.glue.game.session.model.PostGameDecision;
import teamnova.omok.glue.game.session.model.GameSession;
import teamnova.omok.glue.game.session.model.vo.GameSessionId;

public interface GameSessionEventProcessor {
    boolean submitReady(String userId, long requestId);
    boolean submitMove(String userId, long requestId, int x, int y);
    boolean submitPostGameDecision(String userId, long requestId, PostGameDecision decision);
    void cancelAllTimers(GameSessionId sessionId);
    void skipTurnForDisconnected(GameSession session, String userId, int expectedTurnNumber);
}
