package teamnova.omok.tcp;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Sample worker event that performs simple command processing.
 */
final class MessageEvent implements WorkerEvent {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final ClientSession session;
    private final String message;

    MessageEvent(ClientSession session, String message) {
        this.session = session;
        this.message = message;
    }

    @Override
    public void execute(NioReactorServer server) {
        String trimmed = message.trim();
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
        session.enqueueResponse(payload + System.lineSeparator());
        server.enqueueSelectorTask(session::enableWriteInterest);
    }
}
