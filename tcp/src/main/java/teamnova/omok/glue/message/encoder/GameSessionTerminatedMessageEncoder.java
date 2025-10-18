package teamnova.omok.glue.message.encoder;

import java.nio.charset.StandardCharsets;
import java.util.List;

import teamnova.omok.glue.game.session.model.GameSession;

public final class GameSessionTerminatedMessageEncoder {
    private GameSessionTerminatedMessageEncoder() {}

    public static byte[] encode(GameSession session, List<String> disconnected) {
        StringBuilder sb = new StringBuilder(192);
        sb.append('{')
          .append("\"sessionId\":\"").append(session.getId()).append('\"')
          .append(',')
          .append("\"startedAt\":").append(session.getGameStartedAt())
          .append(',')
          .append("\"endedAt\":").append(session.getGameEndedAt())
          .append(',')
          .append("\"durationMillis\":").append(session.getGameDurationMillis())
          .append(',')
          .append("\"turnCount\":").append(session.getCompletedTurnCount())
          .append(',')
          .append("\"disconnected\":[");
        for (int i = 0; i < disconnected.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append('\"').append(MessageEncodingUtil.escape(disconnected.get(i))).append('\"');
        }
        sb.append(']')
          .append('}');
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
}
