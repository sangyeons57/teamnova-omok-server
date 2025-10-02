package teamnova.omok.handler;

import teamnova.omok.handler.register.FrameHandler;
import teamnova.omok.nio.ClientSession;
import teamnova.omok.nio.FramedMessage;
import teamnova.omok.nio.NioReactorServer;

public class PingPongHandler implements FrameHandler {
    public PingPongHandler() { }

    @Override
    public void handle(NioReactorServer server, ClientSession session, FramedMessage frame) {
        session.enqueueResponse(frame.type(), frame.requestId(), frame.payload());
        server.enqueueSelectorTask(session::enableWriteInterest);
    }
}
