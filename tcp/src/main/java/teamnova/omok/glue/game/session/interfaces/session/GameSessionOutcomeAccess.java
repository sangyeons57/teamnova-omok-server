package teamnova.omok.glue.game.session.interfaces.session;

import teamnova.omok.glue.game.session.model.PlayerResult;

public interface GameSessionOutcomeAccess extends  GameSessionAccessInterface {
    void resetOutcomes();
    PlayerResult outcomeFor(String userId);
    void updateOutcome(String userId, PlayerResult result);
    boolean isGameFinished();
}
