package teamnova.omok.glue.handler;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import teamnova.omok.glue.message.decoder.HelloWorldDecoder;
import teamnova.omok.glue.handler.register.FrameHandler;
import teamnova.omok.glue.handler.register.Type;
import teamnova.omok.core.nio.ClientSession;
import teamnova.omok.core.nio.FramedMessage;
import teamnova.omok.core.nio.NioReactorServer;

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
        try {
            System.out.println("[HelloWorldHandler:" + session.remoteAddress() + " ] response: " + response);
        } catch (IOException e) {
            System.err.println("[HelloWorldHandler:] Failed to read remote address: " + e.getMessage());
        }
        session.enqueueResponse(Type.HELLO, frame.requestId(), response.getBytes(StandardCharsets.UTF_8));
        server.enqueueSelectorTask(session::enableWriteInterest);
    }
}
