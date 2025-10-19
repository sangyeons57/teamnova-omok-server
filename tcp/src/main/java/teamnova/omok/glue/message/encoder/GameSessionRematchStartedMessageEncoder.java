package teamnova.omok.glue.message.encoder;

import java.nio.charset.StandardCharsets;
import java.util.List;

import teamnova.omok.glue.game.session.model.GameSession;

public final class GameSessionRematchStartedMessageEncoder {
    private GameSessionRematchStartedMessageEncoder() {}

    public static byte[] encode(GameSession previous, GameSession rematch, List<String> participants) {
        StringBuilder sb = new StringBuilder(192);
        sb.append('{')
          .append("\"sessionId\":\"").append(previous.sessionId().asUuid()).append('\"')
          .append(',')
          .append("\"rematchSessionId\":\"").append(rematch.sessionId().asUuid()).append('\"')
          .append(',')
          .append("\"participants\":[");
        for (int i = 0; i < participants.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append('\"').append(MessageEncodingUtil.escape(participants.get(i))).append('\"');
        }
        sb.append(']')
          .append('}');
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
}
