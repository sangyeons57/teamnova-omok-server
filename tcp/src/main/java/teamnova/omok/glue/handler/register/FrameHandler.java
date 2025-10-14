package teamnova.omok.glue.handler.register;

import teamnova.omok.core.nio.ClientSession;
import teamnova.omok.core.nio.FramedMessage;
import teamnova.omok.core.nio.NioReactorServer;

@FunctionalInterface
public interface FrameHandler {
    void handle(NioReactorServer server, ClientSession session, FramedMessage frame);
}
