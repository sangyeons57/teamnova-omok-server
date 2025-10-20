package teamnova.omok.glue.message.encoder;

import java.nio.charset.StandardCharsets;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionAccess;
import teamnova.omok.glue.game.session.model.result.MoveResult;

public final class StonePlacedMessageEncoder {
    private StonePlacedMessageEncoder() {}

    public static byte[] encode(GameSessionAccess session, MoveResult result) {
        StringBuilder sb = new StringBuilder(224);
        sb.append('{')
          .append("\"sessionId\":\"").append(session.sessionId().asUuid()).append('\"')
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
