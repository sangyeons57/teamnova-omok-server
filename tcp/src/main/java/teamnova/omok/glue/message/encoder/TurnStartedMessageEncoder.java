package teamnova.omok.glue.message.encoder;

import java.nio.charset.StandardCharsets;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionAccess;
import teamnova.omok.glue.game.session.model.dto.TurnSnapshot;

public final class TurnStartedMessageEncoder {
    private TurnStartedMessageEncoder() { }

    public static byte[] encode(GameSessionAccess session, TurnSnapshot snapshot) {
        StringBuilder sb = new StringBuilder(192);
        sb.append('{')
          .append("\"sessionId\":\"").append(session.sessionId().asUuid()).append('\"')
          .append(',')
          .append("\"turn\":");
        MessageEncodingUtil.appendTurn(sb, snapshot);
        sb.append('}');
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
}
