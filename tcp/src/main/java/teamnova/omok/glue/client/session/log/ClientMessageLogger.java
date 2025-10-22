package teamnova.omok.glue.client.session.log;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import teamnova.omok.glue.client.session.interfaces.ClientSessionHandle;
import teamnova.omok.glue.handler.register.Type;

/**
 * Lightweight logger for tracking client-scoped network IO.
 * Complements GameSessionLogger which focuses on in-game session scope.
 */
public final class ClientMessageLogger {
    private static final String PREFIX = "[CLIENT]";
    private static final DateTimeFormatter FORMATTER =
        DateTimeFormatter.ofPattern("HH:mm:ss.SSS").withZone(ZoneId.systemDefault());

    private ClientMessageLogger() { }

    public static void inbound(ClientSessionHandle session,
                               Type type,
                               long requestId,
                               String... details) {
        log(session, "inbound", type, requestId, details);
    }

    public static void outbound(ClientSessionHandle session,
                                Type type,
                                long requestId,
                                String... details) {
        log(session, "outbound", type, requestId, details);
    }

    private static void log(ClientSessionHandle session,
                            String category,
                            Type type,
                            long requestId,
                            String... details) {
        Objects.requireNonNull(category, "category");
        String timestamp = FORMATTER.format(Instant.now());
        String remote = "-";
        if (session != null) {
            try {
                var addr = session.remoteAddress();
                remote = addr != null ? String.valueOf(addr) : "-";
            } catch (Exception ignored) { }
        }
        String user = session != null && session.isAuthenticated() ? session.authenticatedUserId() : null;
        StringBuilder line = new StringBuilder()
            .append(PREFIX)
            .append('[').append(timestamp).append(']')
            .append("[remote=").append(remote).append(']')
            .append('[').append(category).append(']')
            .append('[').append(type != null ? type.name() : "-").append(']');
        if (requestId > 0) {
            line.append(" requestId=").append(requestId);
        }
        if (user != null && !user.isBlank()) {
            line.append(" user=").append(user);
        }
        System.out.println(line);
        if (details != null) {
            for (String d : details) {
                if (d == null || d.isEmpty()) continue;
                String[] lines = d.split("\\R", -1);
                for (String l : lines) {
                    System.out.println("    ↳ " + l);
                }
            }
        }
    }
}
