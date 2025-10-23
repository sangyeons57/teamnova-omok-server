package teamnova.omok.glue.client.session.interfaces;

import teamnova.omok.glue.client.session.interfaces.transport.ManagedSessionTransport;
import teamnova.omok.glue.client.session.interfaces.view.ClientSessionView;
import teamnova.omok.glue.game.session.model.vo.GameSessionId;

public interface ClientSessionHandle extends ManagedSessionTransport, ClientSessionView {
    // Bind the current in-game session to scope outbound traffic
    void bindGameSession(teamnova.omok.glue.game.session.model.vo.GameSessionId id);
    void unbindGameSession(teamnova.omok.glue.game.session.model.vo.GameSessionId id);
    GameSessionId currentGameSessionId();
}
