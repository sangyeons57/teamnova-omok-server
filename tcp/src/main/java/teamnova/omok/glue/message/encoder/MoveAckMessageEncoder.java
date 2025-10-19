package teamnova.omok.glue.message.encoder;

import java.nio.charset.StandardCharsets;

import teamnova.omok.glue.game.session.model.result.MoveResult;
import teamnova.omok.glue.game.session.model.result.MoveStatus;

public final class MoveAckMessageEncoder {
    private MoveAckMessageEncoder() {}

    public static byte[] encode(MoveResult result) {
        StringBuilder sb = new StringBuilder(224);
        sb.append('{')
          .append("\"sessionId\":\"").append(result.session().sessionId().asUuid()).append('\"')
          .append(',')
          .append("\"status\":\"").append(result.status()).append('\"');
        sb.append(',')
          .append("\"x\":").append(result.x())
          .append(',')
          .append("\"y\":").append(result.y());
        if (result.status() == MoveStatus.SUCCESS) {
            sb.append(',')
              .append("\"placedBy\":\"").append(MessageEncodingUtil.escape(result.userId())).append('\"')
              .append(',')
              .append("\"stone\":").append(result.placedAs().code());
        }
        sb.append(',')
          .append("\"turn\":");
        MessageEncodingUtil.appendTurn(sb, result.turnSnapshot());
        sb.append('}');
        System.out.println(sb);
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
}
