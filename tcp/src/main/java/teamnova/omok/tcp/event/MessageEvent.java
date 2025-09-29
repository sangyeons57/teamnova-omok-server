package teamnova.omok.tcp.event;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import teamnova.omok.tcp.nio.ClientSession;
import teamnova.omok.tcp.nio.FramedMessage;
import teamnova.omok.tcp.nio.NioReactorServer;

/**
 * Sample worker event that performs simple command processing.
 */
public final class MessageEvent implements WorkerEvent {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ClientSession session;
    private final FramedMessage frame;

    public MessageEvent(ClientSession session, FramedMessage frame) {
        this.session = session;
        this.frame = frame;
    }

    @Override
    public void execute(NioReactorServer server) {
        String trimmed = new String(frame.payload(), StandardCharsets.UTF_8).trim();
        if (trimmed.isEmpty()) {
            return;
        }
        switch (trimmed.toLowerCase()) {
            case "quit", "exit" -> server.enqueueSelectorTask(() -> server.closeSession(session));
            case "time" -> respond(server, "server time " + FORMATTER.format(LocalDateTime.now()));
            default -> respond(server, "echo " + trimmed);
        }
    }

    private void respond(NioReactorServer server, String payload) {
        byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
        session.enqueueResponse(frame.type(), frame.requestId(), bytes);
        server.enqueueSelectorTask(session::enableWriteInterest);
    }
}
