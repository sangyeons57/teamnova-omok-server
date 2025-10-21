package teamnova.omok.glue.message.encoder;

import java.nio.charset.StandardCharsets;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionAccess;
import teamnova.omok.glue.game.session.model.runtime.TurnPersonalFrame;

public final class TurnTimeoutMessageEncoder {
    private TurnTimeoutMessageEncoder() {}

    public static byte[] encode(GameSessionAccess session, TurnPersonalFrame frame) {
        StringBuilder sb = new StringBuilder(192);
        sb.append('{')
          .append("\"sessionId\":\"").append(session.sessionId().asUuid()).append('\"')
          .append(',')
          .append("\"timedOutUserId\":");
        String previousPlayerId = frame.timeoutPreviousPlayerId();
        if (previousPlayerId == null) {
            sb.append("null");
        } else {
            sb.append('\"').append(MessageEncodingUtil.escape(previousPlayerId)).append('\"');
        }
        sb.append(',')
          .append("\"turn\":");
        MessageEncodingUtil.appendTurn(sb, frame.timeoutSnapshot());
        sb.append('}');
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
}
