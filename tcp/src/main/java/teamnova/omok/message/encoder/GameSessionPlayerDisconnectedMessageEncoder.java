package teamnova.omok.message.encoder;

import java.nio.charset.StandardCharsets;

import teamnova.omok.domain.session.game.GameSession;

/**
 * Encodes a per-session notification indicating that a player has disconnected or left.
 */
public final class GameSessionPlayerDisconnectedMessageEncoder {
    private GameSessionPlayerDisconnectedMessageEncoder() { }

    public static byte[] encode(GameSession session, String userId, String reason) {
        StringBuilder sb = new StringBuilder(128);
        sb.append('{')
          .append("\"sessionId\":\"").append(session.getId()).append('\"')
          .append(',')
          .append("\"userId\":\"").append(MessageEncodingUtil.escape(userId)).append('\"');
        if (reason != null && !reason.isBlank()) {
            sb.append(',')
              .append("\"reason\":\"").append(MessageEncodingUtil.escape(reason)).append('\"');
        }
        sb.append('}');
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
}
