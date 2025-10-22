package teamnova.omok.glue.game.session.log;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionAccess;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContext;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateType;
import teamnova.omok.glue.handler.register.Type;

/**
 * Lightweight structured logger for tracking in-game session activity via System.out.
 * Captures both state-machine transitions and network IO for quick troubleshooting.
 */
public final class GameSessionLogger {
    private static final String PREFIX = "[SESSION]";
    private static final DateTimeFormatter FORMATTER =
        DateTimeFormatter.ofPattern("HH:mm:ss.SSS").withZone(ZoneId.systemDefault());

    private GameSessionLogger() { }

    public static void transition(GameSessionStateContext context,
                                  GameSessionStateType from,
                                  GameSessionStateType to,
                                  String trigger,
                                  String... details) {
        String message = String.format("transition %s -> %s (trigger=%s)",
            stateLabel(from), stateLabel(to), trigger != null ? trigger : "unknown");
        logState(context, "transition", stateLabel(to), message, details);
    }

    public static void signal(GameSessionStateContext context,
                              GameSessionStateType state,
                              teamnova.omok.modules.state_machine.models.LifecycleEventKind kind,
                              String... details) {
        String message = "signal=" + (kind != null ? kind.name() : "-");
        logState(context, "signal", stateLabel(state), message, details);
    }

    public static void enter(GameSessionStateContext context,
                             GameSessionStateType state,
                             String... details) {
        logState(context, "enter", stateLabel(state), "enter state", details);
    }

    public static void exit(GameSessionStateContext context,
                            GameSessionStateType state,
                            String... details) {
        logState(context, "exit", stateLabel(state), "exit state", details);
    }

    public static void event(GameSessionStateContext context,
                             GameSessionStateType state,
                             String eventName,
                             String... details) {
        String message = "event=" + eventName;
        logState(context, "event", stateLabel(state), message, details);
    }

    public static void inbound(GameSessionAccess session,
                               String action,
                               String userId,
                               long requestId,
                               String... details) {
        logIo(session, "inbound", action, userId, requestId, null, details);
    }

    public static void outbound(GameSessionAccess session,
                                Type type,
                                String channel,
                                long requestId,
                                Collection<String> recipients,
                                String... details) {
        logIo(session, "outbound", renderAction(type, channel), null, requestId, recipients, details);
    }

    private static void logState(GameSessionStateContext context,
                                 String category,
                                 String state,
                                 String message,
                                 String... details) {
        String sessionId = context != null
            ? context.session().sessionId().asUuid().toString()
            : "-";
        log(sessionId, category, state, message, details);
    }

    private static void logIo(GameSessionAccess session,
                              String category,
                              String action,
                              String userId,
                              long requestId,
                              Collection<String> recipients,
                              String... details) {
        Objects.requireNonNull(category, "category");
        String sessionId = session != null
            ? session.sessionId().asUuid().toString()
            : "-";
        StringBuilder message = new StringBuilder(action != null ? action : "-");
        if (requestId > 0) {
            message.append(" requestId=").append(requestId);
        }
        if (userId != null) {
            message.append(" user=").append(userId);
        }
        if (recipients != null && !recipients.isEmpty()) {
            message.append(" recipients=").append(renderRecipients(recipients));
        }
        log(sessionId, category, null, message.toString(), details);
    }

    private static void log(String sessionId,
                            String category,
                            String state,
                            String message,
                            String... details) {
        String timestamp = FORMATTER.format(Instant.now());
        StringBuilder line = new StringBuilder()
            .append(PREFIX)
            .append('[').append(timestamp).append(']')
            .append("[session=").append(sessionId != null ? sessionId : "-").append(']')
            .append('[').append(category != null ? category : "log").append(']');
        if (state != null) {
            line.append("[state=").append(state).append(']');
        }
        if (message != null && !message.isEmpty()) {
            line.append(' ').append(message);
        }
        System.out.println(line);
        if (details != null) {
            for (String detail : details) {
                if (detail == null || detail.isEmpty()) {
                    continue;
                }
                String[] lines = detail.split("\\R", -1);
                for (String l : lines) {
                    System.out.println("    ↳ " + l);
                }
            }
        }
    }

    private static String renderAction(Type type, String channel) {
        String resolvedType = type != null ? type.name() : "UNKNOWN";
        return channel != null ? channel + ":" + resolvedType : resolvedType;
    }

    private static String renderRecipients(Collection<String> recipients) {
        if (recipients.size() <= 4) {
            return recipients.stream().collect(Collectors.joining(","));
        }
        return recipients.size() + " recipients";
    }

    private static String stateLabel(GameSessionStateType state) {
        return state != null ? state.name() : "NONE";
    }
}
