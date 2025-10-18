package teamnova.omok.glue.game.session.interfaces;

import java.util.Optional;

import teamnova.omok.core.nio.NioReactorServer;
import teamnova.omok.glue.game.session.model.PostGameDecision;
import teamnova.omok.glue.game.session.model.GameSession;
import teamnova.omok.modules.matching.models.MatchGroup;

public interface GameSessionOperations {
    void attachServer(NioReactorServer server);
    Optional<GameSession> findByUser(String userId);
    void leaveByUser(String userId);
    void handleClientDisconnected(String userId);
    boolean submitReady(String userId, long requestId);
    boolean submitMove(String userId, long requestId, int x, int y);
    boolean submitPostGameDecision(String userId, long requestId, PostGameDecision decision);
    void createFromGroup(NioReactorServer server, MatchGroup group);
}
