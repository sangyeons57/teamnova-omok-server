package teamnova.omok.glue.handler;

import teamnova.omok.glue.handler.register.FrameHandler;
import teamnova.omok.glue.handler.register.Type;
import teamnova.omok.core.nio.ClientSession;
import teamnova.omok.core.nio.FramedMessage;
import teamnova.omok.core.nio.NioReactorServer;

public class PingPongHandler implements FrameHandler {
    public PingPongHandler() { }

    @Override
    public void handle(NioReactorServer server, ClientSession session, FramedMessage frame) {
        session.enqueueResponse(Type.PINGPONG, frame.requestId(), frame.payload());
        server.enqueueSelectorTask(session::enableWriteInterest);
    }
}
