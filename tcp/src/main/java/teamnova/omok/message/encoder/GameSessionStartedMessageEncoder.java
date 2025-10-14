package teamnova.omok.message.encoder;

import java.nio.charset.StandardCharsets;

import teamnova.omok.domain.session.game.GameSession;
import teamnova.omok.domain.session.game.entity.board.BoardService;
import teamnova.omok.domain.session.game.entity.turn.TurnSnapshot;

public final class GameSessionStartedMessageEncoder {
    private GameSessionStartedMessageEncoder() {}

    public static byte[] encode(GameSession session, TurnSnapshot turn) {
        StringBuilder sb = new StringBuilder(256);
        sb.append('{')
          .append("\"sessionId\":\"").append(session.getId()).append('\"')
          .append(',')
          .append("\"startedAt\":").append(session.getGameStartedAt())
          .append(',')
          .append("\"board\":{")
          .append("\"width\":").append(BoardService.DEFAULT_WIDTH).append(',')
          .append("\"height\":").append(BoardService.DEFAULT_HEIGHT)
          .append('}')
          .append(',')
          .append("\"turn\":");
        MessageEncodingUtil.appendTurn(sb, turn);
        sb.append('}');
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
}
