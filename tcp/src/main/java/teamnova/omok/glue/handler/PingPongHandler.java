package teamnova.omok.glue.handler;

import teamnova.omok.glue.client.session.interfaces.ClientSessionHandle;
import teamnova.omok.glue.handler.register.FrameHandler;
import teamnova.omok.core.nio.FramedMessage;
import teamnova.omok.core.nio.NioReactorServer;

public class PingPongHandler implements FrameHandler {
    public PingPongHandler() { }

    @Override
    public void handle(NioReactorServer server, ClientSessionHandle session, FramedMessage frame) {
        teamnova.omok.glue.client.session.log.ClientMessageLogger.inbound(session, teamnova.omok.glue.handler.register.Type.PINGPONG, frame.requestId());
        session.sendPingPong(frame.requestId(), frame.payload());
    }
}
