package teamnova.omok.glue.handler.register;

import teamnova.omok.glue.client.session.interfaces.ClientSessionHandle;
import teamnova.omok.core.nio.FramedMessage;
import teamnova.omok.core.nio.NioReactorServer;

@FunctionalInterface
public interface FrameHandler {
    void handle(NioReactorServer server, ClientSessionHandle session, FramedMessage frame);
}
