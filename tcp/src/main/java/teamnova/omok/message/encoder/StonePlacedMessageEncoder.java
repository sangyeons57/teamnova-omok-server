package teamnova.omok.message.encoder;

import java.nio.charset.StandardCharsets;

import teamnova.omok.service.dto.MoveResult;

public final class StonePlacedMessageEncoder {
    private StonePlacedMessageEncoder() {}

    public static byte[] encode(MoveResult result) {
        StringBuilder sb = new StringBuilder(224);
        sb.append('{')
          .append("\"sessionId\":\"").append(result.session().getId()).append('\"')
          .append(',')
          .append("\"x\":").append(result.x())
          .append(',')
          .append("\"y\":").append(result.y())
          .append(',')
          .append("\"placedBy\":\"").append(MessageEncodingUtil.escape(result.userId())).append('\"')
          .append(',')
          .append("\"stone\":").append(result.placedAs().code())
          .append(',')
          .append("\"turn\":");
        MessageEncodingUtil.appendTurn(sb, result.turnSnapshot());
        sb.append('}');
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
}
