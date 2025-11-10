package teamnova.omok.glue.client.session.interfaces;

import java.util.Set;

import teamnova.omok.glue.client.session.interfaces.transport.ManagedSessionTransport;
import teamnova.omok.glue.client.session.interfaces.view.ClientSessionView;
import teamnova.omok.glue.game.session.model.vo.GameSessionId;

public interface ClientSessionHandle extends ManagedSessionTransport, ClientSessionView {
    // Bind the current in-game session to scope outbound traffic
    void bindGameSession(GameSessionId id);
    void unbindGameSession(GameSessionId id);
    GameSessionId currentGameSessionId();

    void requestMatchmaking(long requestId, int rating, Set<Integer> matchSizes);

    void cancelMatchmaking(long requestId);
}
