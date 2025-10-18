package teamnova.omok.glue.game.session.interfaces;

import teamnova.omok.glue.game.session.model.vo.GameSessionId;

public interface DecisionTimeoutScheduler {
    void schedule(GameSessionId sessionId, long deadlineAt, Runnable task);
    void cancel(GameSessionId sessionId);
}
