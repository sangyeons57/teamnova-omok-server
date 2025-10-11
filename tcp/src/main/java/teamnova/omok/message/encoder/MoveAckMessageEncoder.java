package teamnova.omok.message.encoder;

import java.nio.charset.StandardCharsets;

import teamnova.omok.service.dto.MoveResult;
import teamnova.omok.service.dto.MoveStatus;

public final class MoveAckMessageEncoder {
    private MoveAckMessageEncoder() {}

    public static byte[] encode(MoveResult result) {
        StringBuilder sb = new StringBuilder(224);
        sb.append('{')
          .append("\"sessionId\":\"").append(result.session().getId()).append('\"')
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
        System.out.println(sb.toString());
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
}
