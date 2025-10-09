package teamnova.omok.message.encoder;

import java.nio.charset.StandardCharsets;
import teamnova.omok.service.TurnService;
import teamnova.omok.store.GameSession;

public final class GameSessionStartedMessageEncoder {
    private GameSessionStartedMessageEncoder() {}

    public static byte[] encode(GameSession session, TurnService.TurnSnapshot turn) {
        StringBuilder sb = new StringBuilder(256);
        sb.append('{')
          .append("\"sessionId\":\"").append(session.getId()).append('\"')
          .append(',')
          .append("\"startedAt\":").append(session.getGameStartedAt())
          .append(',')
          .append("\"board\":{")
          .append("\"width\":").append(GameSession.BOARD_WIDTH).append(',')
          .append("\"height\":").append(GameSession.BOARD_HEIGHT)
          .append('}')
          .append(',')
          .append("\"turn\":");
        MessageEncodingUtil.appendTurn(sb, turn);
        sb.append('}');
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
}
