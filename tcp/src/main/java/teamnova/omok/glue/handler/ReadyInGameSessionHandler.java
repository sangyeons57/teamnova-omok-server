package teamnova.omok.glue.handler;

import teamnova.omok.glue.client.session.ClientSessionManager;
import teamnova.omok.glue.client.session.interfaces.ClientSessionHandle;
import teamnova.omok.glue.handler.register.FrameHandler;
import teamnova.omok.glue.handler.register.Type;
import teamnova.omok.core.nio.FramedMessage;
import teamnova.omok.core.nio.NioReactorServer;
import teamnova.omok.glue.game.session.GameSessionManager;

public class ReadyInGameSessionHandler implements FrameHandler {
    @Override
    public void handle(NioReactorServer server, ClientSessionHandle session, FramedMessage frame) {
        teamnova.omok.glue.client.session.log.ClientMessageLogger.inbound(session, teamnova.omok.glue.handler.register.Type.READY_IN_GAME_SESSION, frame.requestId());
        if (!session.isAuthenticated()) {
            return;
        }
        String userId = session.authenticatedUserId();
        GameSessionManager gameSessionManager = GameSessionManager.getInstance();

        boolean accepted = gameSessionManager.submitReady(userId, frame.requestId());
        if (!accepted) {
            ClientSessionManager.getInstance().gamePublisher()
                .respondError(userId, Type.READY_IN_GAME_SESSION, frame.requestId(), "SESSION_NOT_FOUND");
        }
    }
}
