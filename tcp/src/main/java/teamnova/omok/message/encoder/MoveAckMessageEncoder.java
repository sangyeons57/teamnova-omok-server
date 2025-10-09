package teamnova.omok.message.encoder;

import java.nio.charset.StandardCharsets;
import teamnova.omok.service.InGameSessionService;

public final class MoveAckMessageEncoder {
    private MoveAckMessageEncoder() {}

    public static byte[] encode(InGameSessionService.MoveResult result) {
        StringBuilder sb = new StringBuilder(224);
        sb.append('{')
          .append("\"sessionId\":\"").append(result.session().getId()).append('\"')
          .append(',')
          .append("\"status\":\"").append(result.status()).append('\"');
        sb.append(',')
          .append("\"x\":").append(result.x())
          .append(',')
          .append("\"y\":").append(result.y());
        if (result.status() == InGameSessionService.MoveStatus.SUCCESS) {
            sb.append(',')
              .append("\"placedBy\":\"").append(MessageEncodingUtil.escape(result.userId())).append('\"')
              .append(',')
              .append("\"stone\":").append(result.placedAs().code());
        }
        sb.append(',')
          .append("\"turn\":");
        MessageEncodingUtil.appendTurn(sb, result.turnSnapshot());
        sb.append('}');
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
}
