package teamnova.omok.glue.client.session.interfaces;

import java.util.Set;

import teamnova.omok.glue.client.state.model.ClientStateTypeTransition;
import teamnova.omok.glue.game.session.states.GameStateHub;
import teamnova.omok.glue.game.session.model.PostGameDecision;
import teamnova.omok.glue.game.session.model.vo.GameSessionId;
import teamnova.omok.glue.client.session.model.AuthResultStatus;

public interface ClientSessionHandle extends ManagedSessionTransport, ClientSessionView {
    // Bind the current in-game session to scope outbound traffic
    void bindGameSession(GameSessionId id);
    void unbindGameSession(GameSessionId id);
    GameSessionId currentGameSessionId();

    void requestMatchmaking(long requestId, int rating, Set<Integer> matchSizes);

    void cancelMatchmaking(long requestId);

    void authenticateUser(String userId, String role, String scope);

    void clearAuthenticationBinding();

    void sendAuthResult(long requestId, AuthResultStatus status);

    void sendHello(long requestId, String response);

    void sendPingPong(long requestId, byte[] payload);

    boolean submitMove(long requestId, int x, int y);

    void sendPlaceStoneError(long requestId, String message);

    void submitReady(long requestId);

    void leaveInGameSession(long requestId);

    void submitPostGameDecision(long requestId, PostGameDecision decision);

    boolean reconnectGameSession();

    void addStateListener(ClientStateTypeTransition typeTransition, ClientSessionStateListener listener);

    void enterGameSession(GameStateHub manager);

    void exitGameSession();

    /**
     * Closes only the underlying transport (socket/channel) without triggering full
     * session termination. Used when a new connection is taking over the session.
     */
    void shutdownTransport();
}
