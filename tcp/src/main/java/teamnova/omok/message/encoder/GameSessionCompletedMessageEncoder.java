package teamnova.omok.message.encoder;

import java.nio.charset.StandardCharsets;
import java.util.List;

import teamnova.omok.game.PlayerResult;
import teamnova.omok.store.GameSession;

/**
 * Encodes the final per-player outcomes for a completed game session.
 */
public final class GameSessionCompletedMessageEncoder {
    private GameSessionCompletedMessageEncoder() {}

    public static byte[] encode(GameSession session) {
        StringBuilder sb = new StringBuilder(256);
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
          .append("\"outcomes\":[");
        List<String> userIds = session.getUserIds();
        for (int i = 0; i < userIds.size(); i++) {
            String userId = userIds.get(i);
            PlayerResult result = session.outcomeFor(userId);
            if (i > 0) {
                sb.append(',');
            }
            sb.append('{')
              .append("\"userId\":\"").append(MessageEncodingUtil.escape(userId)).append('\"')
              .append(',')
              .append("\"result\":\"").append(result.name()).append('\"')
              .append('}');
        }
        sb.append(']')
          .append('}');
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
}
