package teamnova.omok.glue.game.session.interfaces.manager;

import teamnova.omok.glue.game.session.interfaces.GameTurnService;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionParticipantsAccess;
import teamnova.omok.glue.game.session.model.GameSession;
import teamnova.omok.glue.game.session.model.dto.TurnSnapshot;
import teamnova.omok.glue.game.session.model.vo.GameSessionId;

public interface TurnTimeoutScheduler {
    void schedule(GameSessionParticipantsAccess session,
                  TurnSnapshot turnSnapshot,
                  TurnTimeoutConsumer consumer);
    void cancel(GameSessionId sessionId);
    boolean validate(GameSessionId sessionId, int expectedTurnNumber);
    void clearIfMatches(GameSessionId sessionId, int expectedTurnNumber);

    interface TurnTimeoutConsumer {
        void onTimeout(GameSessionId sessionId, int expectedTurnNumber);
    }
}
