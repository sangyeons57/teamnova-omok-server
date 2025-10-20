package teamnova.omok.glue.game.session.interfaces.session;

import teamnova.omok.glue.game.session.model.runtime.TurnTransition;
import teamnova.omok.glue.game.session.states.manage.TurnCycleContext;
import teamnova.omok.glue.game.session.model.result.MoveResult;
import teamnova.omok.glue.game.session.model.result.ReadyResult;
import teamnova.omok.glue.game.session.model.result.TurnTimeoutResult;

public interface GameSessionTurnRuntimeAccess {
    TurnCycleContext getActiveTurnCycle();

    void setActiveTurnCycle(TurnCycleContext context);

    void clearActiveTurnCycle();

    MoveResult getPendingMoveResult();

    void setPendingMoveResult(MoveResult result);

    void clearPendingMoveResult();

    ReadyResult getPendingReadyResult();

    void setPendingReadyResult(ReadyResult result);

    void clearPendingReadyResult();

    TurnTimeoutResult getPendingTimeoutResult();

    void setPendingTimeoutResult(TurnTimeoutResult result);

    void clearPendingTimeoutResult();

    TurnTransition getPendingTurnTransition();

    void setPendingTurnTransition(TurnTransition transition);

    void clearPendingTurnTransition();
}
