package teamnova.omok.glue.message.encoder;

import java.nio.charset.StandardCharsets;

import teamnova.omok.glue.game.session.interfaces.GameTurnService;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionLifecycleAccess;
import teamnova.omok.glue.game.session.model.GameSession;

public final class GameSessionStartedMessageEncoder {
    private GameSessionStartedMessageEncoder() {}

    public static byte[] encode(GameSessionLifecycleAccess session, GameTurnService.TurnSnapshot turn) {
        StringBuilder sb = new StringBuilder(256);
        sb.append('{')
          .append("\"sessionId\":\"").append(session.sessionId().asUuid()).append('\"')
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
