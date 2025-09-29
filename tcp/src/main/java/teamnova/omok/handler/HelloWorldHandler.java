package teamnova.omok.handler;

import java.nio.charset.StandardCharsets;
import teamnova.omok.decoder.HelloWorldDecoder;
import teamnova.omok.nio.ClientSession;
import teamnova.omok.nio.FramedMessage;
import teamnova.omok.nio.NioReactorServer;

public final class HelloWorldHandler implements FrameHandler {
    private final HelloWorldDecoder decoder;

    public HelloWorldHandler(HelloWorldDecoder decoder) {
        this.decoder = decoder;
    }

    @Override
    public void handle(NioReactorServer server, ClientSession session, FramedMessage frame) {
        String request = decoder.decode(frame.payload()).trim();
        if (request.isEmpty()) {
            return;
        }
        String response = "hello " + request;
        session.enqueueResponse(frame.type(), frame.requestId(), response.getBytes(StandardCharsets.UTF_8));
        server.enqueueSelectorTask(session::enableWriteInterest);
    }
}
