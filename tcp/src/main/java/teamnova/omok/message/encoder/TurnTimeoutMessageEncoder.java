package teamnova.omok.message.encoder;

import java.nio.charset.StandardCharsets;
import teamnova.omok.service.InGameSessionService;

public final class TurnTimeoutMessageEncoder {
    private TurnTimeoutMessageEncoder() {}

    public static byte[] encode(InGameSessionService.TurnTimeoutResult result) {
        StringBuilder sb = new StringBuilder(192);
        sb.append('{')
          .append("\"sessionId\":\"").append(result.session().getId()).append('\"')
          .append(',')
          .append("\"timedOutUserId\":");
        if (result.previousPlayerId() == null) {
            sb.append("null");
        } else {
            sb.append('\"').append(MessageEncodingUtil.escape(result.previousPlayerId())).append('\"');
        }
        sb.append(',')
          .append("\"turn\":");
        MessageEncodingUtil.appendTurn(sb, result.nextTurn());
        sb.append('}');
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
}
