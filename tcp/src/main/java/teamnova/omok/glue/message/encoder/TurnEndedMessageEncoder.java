package teamnova.omok.glue.message.encoder;

import java.nio.charset.StandardCharsets;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionAccess;
import teamnova.omok.glue.game.session.model.result.MoveStatus;
import teamnova.omok.glue.game.session.model.runtime.TurnPersonalFrame;

public final class TurnEndedMessageEncoder {
    private TurnEndedMessageEncoder() { }

    public static byte[] encode(GameSessionAccess session, TurnPersonalFrame frame) {
        boolean moveSuccess = frame.outcomeStatus() == MoveStatus.SUCCESS;
        boolean timedOut = frame.timeoutTimedOut();
        String cause;
        if (moveSuccess) {
            cause = "MOVE";
        } else if (timedOut) {
            cause = "TIMEOUT";
        } else {
            cause = "UNKNOWN";
        }

        StringBuilder sb = new StringBuilder(256);
        sb.append('{')
          .append("\"sessionId\":\"").append(session.sessionId().asUuid()).append('\"')
          .append(',')
          .append("\"cause\":\"").append(cause).append('\"')
          .append(',')
          .append("\"playerId\":").append('\"').append(MessageEncodingUtil.escape(frame.userId())).append('\"');
        sb.append(',')
          .append("\"status\":");
        if (frame.outcomeStatus() != null) {
            sb.append('\"').append(frame.outcomeStatus().name()).append('\"');
        } else if (timedOut) {
            sb.append("\"TIMEOUT\"");
        } else {
            sb.append("null");
        }
        sb.append(',')
          .append("\"timedOut\":").append(timedOut);

        if (moveSuccess) {
            sb.append(',')
              .append("\"move\":{")
              .append("\"x\":").append(frame.x())
              .append(',')
              .append("\"y\":").append(frame.y())
              .append(',')
              .append("\"stone\":");
            if (frame.stone() != null) {
                sb.append(frame.stone().code());
            } else {
                sb.append("null");
            }
            sb.append('}');
        } else {
            sb.append(',')
              .append("\"move\":null");
        }

        sb.append('}');
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
}
