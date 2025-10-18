package teamnova.omok.glue.game.session.interfaces;

import teamnova.omok.glue.game.session.model.GameSession;
import teamnova.omok.glue.game.session.model.vo.GameSessionId;

public interface TurnTimeoutScheduler {
    void schedule(GameSession session,
                  GameTurnService.TurnSnapshot turnSnapshot,
                  TurnTimeoutConsumer consumer);
    void cancel(GameSessionId sessionId);
    boolean validate(GameSessionId sessionId, int expectedTurnNumber);
    void clearIfMatches(GameSessionId sessionId, int expectedTurnNumber);

    interface TurnTimeoutConsumer {
        void onTimeout(GameSessionId sessionId, int expectedTurnNumber);
    }
}
