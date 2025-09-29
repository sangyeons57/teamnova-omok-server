package teamnova.omok.handler;

import teamnova.omok.nio.ClientSession;
import teamnova.omok.nio.FramedMessage;
import teamnova.omok.nio.NioReactorServer;

@FunctionalInterface
public interface FrameHandler {
    void handle(NioReactorServer server, ClientSession session, FramedMessage frame) throws Exception;
}
