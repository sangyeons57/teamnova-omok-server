package teamnova.omok.glue.client.session.interfaces;

import teamnova.omok.glue.client.session.model.ClientSession;
import teamnova.omok.glue.client.state.ClientStateHub;
import teamnova.omok.glue.game.session.model.PlayerResult;
import teamnova.omok.glue.handler.register.Type;

/**
 * Authenticated session facade used by handlers and publishers.
 */
public interface ClientSessionView {
    void enqueueResponse(Type type, long requestId, byte[] payload);

    boolean isAuthenticated();

    String authenticatedUserId();

    void markAuthenticated(String userId, String role, String scope);

    void clearAuthentication();

    ClientStateHub getStateHub();

    ClientSession model();

    void attachClientSession(ClientSessionHandle clientSessionHandle);

    ClientSession.ClientSessionMetrics registerOutcome(PlayerResult result);
}
