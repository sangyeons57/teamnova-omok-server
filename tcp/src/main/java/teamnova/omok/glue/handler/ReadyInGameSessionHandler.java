package teamnova.omok.glue.handler;

import teamnova.omok.glue.client.session.interfaces.ClientSessionHandle;
import teamnova.omok.glue.client.session.log.ClientMessageLogger;
import teamnova.omok.glue.handler.register.FrameHandler;
import teamnova.omok.core.nio.FramedMessage;
import teamnova.omok.core.nio.NioReactorServer;

public class ReadyInGameSessionHandler implements FrameHandler {
    @Override
    public void handle(NioReactorServer server, ClientSessionHandle session, FramedMessage frame) {
        ClientMessageLogger.inbound(session, teamnova.omok.glue.handler.register.Type.READY_IN_GAME_SESSION, frame.requestId());
        if (!session.isAuthenticated()) {
            return;
        }
        session.submitReady(frame.requestId());
    }
}
