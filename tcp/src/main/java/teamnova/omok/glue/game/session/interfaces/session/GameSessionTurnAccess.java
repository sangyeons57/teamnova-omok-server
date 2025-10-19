package teamnova.omok.glue.game.session.interfaces.session;

import teamnova.omok.glue.game.session.model.vo.TurnCounters;
import teamnova.omok.glue.game.session.model.vo.TurnOrder;
import teamnova.omok.glue.game.session.model.vo.TurnTiming;

/**
 * Accessor contract exposed by {@code GameSession} for turn state interactions.
 */
public interface GameSessionTurnAccess extends GameSessionAccessInterface {
    TurnOrder order();
    void order(TurnOrder order);
    TurnCounters counters();
    void counters(TurnCounters counters);
    int actionNumber();
    int getCurrentPlayerIndex();
    void setCurrentPlayerIndex(int currentPlayerIndex);
    TurnTiming timing();
    void timing(TurnTiming timing);
    void reset();
}
