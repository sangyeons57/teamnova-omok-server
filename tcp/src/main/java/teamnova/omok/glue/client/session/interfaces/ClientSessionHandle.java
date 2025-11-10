package teamnova.omok.glue.client.session.interfaces;

import java.util.Set;

import teamnova.omok.glue.client.session.interfaces.transport.ManagedSessionTransport;
import teamnova.omok.glue.client.session.interfaces.view.ClientSessionView;
import teamnova.omok.glue.game.session.model.PostGameDecision;
import teamnova.omok.glue.game.session.model.vo.GameSessionId;

public interface ClientSessionHandle extends ManagedSessionTransport, ClientSessionView {
    // Bind the current in-game session to scope outbound traffic
    void bindGameSession(GameSessionId id);
    void unbindGameSession(GameSessionId id);
    GameSessionId currentGameSessionId();

    void requestMatchmaking(long requestId, int rating, Set<Integer> matchSizes);

    void cancelMatchmaking(long requestId);

    void authenticateUser(String userId, String role, String scope);

    void clearAuthenticationBinding();

    void sendAuthResult(long requestId, boolean success);

    void sendHello(long requestId, String response);

    void sendPingPong(long requestId, byte[] payload);

    boolean submitMove(long requestId, int x, int y);

    void sendPlaceStoneError(long requestId, String message);

    void submitReady(long requestId);

    void leaveInGameSession(long requestId);

    void submitPostGameDecision(long requestId, PostGameDecision decision);

    boolean reconnectGameSession(GameSessionId sessionId);

    void sendReconnectResult(long requestId, boolean success, String detail);

    void beginReconnectFlow();

    void finishReconnectFlow(boolean success);
}
