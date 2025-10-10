package teamnova.omok.message.encoder;

import java.nio.charset.StandardCharsets;
import java.util.List;

import teamnova.omok.store.GameSession;

public final class GameSessionRematchStartedMessageEncoder {
    private GameSessionRematchStartedMessageEncoder() {}

    public static byte[] encode(GameSession previous, GameSession rematch, List<String> participants) {
        StringBuilder sb = new StringBuilder(192);
        sb.append('{')
          .append("\"sessionId\":\"").append(previous.getId()).append('\"')
          .append(',')
          .append("\"rematchSessionId\":\"").append(rematch.getId()).append('\"')
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
