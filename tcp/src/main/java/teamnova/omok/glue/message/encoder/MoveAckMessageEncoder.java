package teamnova.omok.glue.message.encoder;

import java.nio.charset.StandardCharsets;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionAccess;
import teamnova.omok.glue.game.session.model.result.MoveStatus;
import teamnova.omok.glue.game.session.model.runtime.TurnPersonalFrame;

public final class MoveAckMessageEncoder {
    private MoveAckMessageEncoder() {}

    public static byte[] encode(GameSessionAccess session, TurnPersonalFrame frame) {
        StringBuilder sb = new StringBuilder(224);
        sb.append('{')
          .append("\"sessionId\":\"").append(session.sessionId().asUuid()).append('\"')
          .append(',')
          .append("\"status\":\"").append(frame.outcomeStatus()).append('\"');
        sb.append(',')
          .append("\"x\":").append(frame.x())
          .append(',')
          .append("\"y\":").append(frame.y());
        if (frame.outcomeStatus() == MoveStatus.SUCCESS) {
            sb.append(',')
              .append("\"placedBy\":\"").append(MessageEncodingUtil.escape(frame.userId())).append('\"')
              .append(',')
              .append("\"stone\":").append(frame.stone() != null ? frame.stone().code() : 0);
        }
        sb.append(',')
          .append("\"turn\":");
        MessageEncodingUtil.appendTurn(sb, frame.outcomeSnapshot());
        sb.append('}');
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
}
