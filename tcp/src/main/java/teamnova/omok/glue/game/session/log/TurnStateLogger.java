package teamnova.omok.glue.game.session.log;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import teamnova.omok.glue.game.session.states.manage.GameSessionStateContext;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateType;

/**
 * Lightweight structured logger for tracking turn-state activity via System.out.
 */
public final class TurnStateLogger {
    private static final String PREFIX = "[TURN]";
    private static final DateTimeFormatter FORMATTER =
        DateTimeFormatter.ofPattern("HH:mm:ss.SSS").withZone(ZoneId.systemDefault());

    private TurnStateLogger() { }

    public static void transition(GameSessionStateContext context,
                                  GameSessionStateType from,
                                  GameSessionStateType to,
                                  String trigger,
                                  String... details) {
        String message = String.format("transition %s -> %s (trigger=%s)",
            stateLabel(from), stateLabel(to), trigger != null ? trigger : "unknown");
        log(context, "transition", stateLabel(to), message, details);
    }

    public static void enter(GameSessionStateContext context,
                             GameSessionStateType state,
                             String... details) {
        log(context, "enter", stateLabel(state), "enter state", details);
    }

    public static void exit(GameSessionStateContext context,
                            GameSessionStateType state,
                            String... details) {
        log(context, "exit", stateLabel(state), "exit state", details);
    }

    public static void event(GameSessionStateContext context,
                             GameSessionStateType state,
                             String eventName,
                             String... details) {
        String message = "event=" + eventName;
        log(context, "event", stateLabel(state), message, details);
    }

    private static void log(GameSessionStateContext context,
                            String category,
                            String state,
                            String message,
                            String... details) {
        String timestamp = FORMATTER.format(Instant.now());
        String sessionId = context != null
            ? context.session().sessionId().asUuid().toString()
            : "-";
        StringBuilder line = new StringBuilder()
            .append(PREFIX)
            .append('[').append(timestamp).append(']')
                .append("[session=").append(sessionId).append(']')
                .append('[').append(category).append(']');
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

    private static String stateLabel(GameSessionStateType state) {
        return state != null ? state.name() : "NONE";
    }
}
