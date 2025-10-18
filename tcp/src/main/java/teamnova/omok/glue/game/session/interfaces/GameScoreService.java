package teamnova.omok.glue.game.session.interfaces;

import teamnova.omok.glue.game.session.model.GameSession;

public interface GameScoreService {
    int calculateScoreDelta(GameSession session, String userId);
}
