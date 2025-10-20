package teamnova.omok.glue.message.encoder;

import java.nio.charset.StandardCharsets;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionAccess;
import teamnova.omok.glue.game.session.model.result.ReadyResult;

public final class ReadyStateMessageEncoder {
    private ReadyStateMessageEncoder() {}

    public static byte[] encode(GameSessionAccess session, ReadyResult result) {
        StringBuilder sb = new StringBuilder(192);
        sb.append('{')
          .append("\"sessionId\":\"").append(session.sessionId().asUuid()).append('\"')
          .append(',');
        if (result.userId() != null) {
            sb.append("\"userId\":\"").append(MessageEncodingUtil.escape(result.userId())).append('\"')
              .append(',');
        }
        boolean ready = result.userId() != null && session.isReady(result.userId());
        sb.append("\"ready\":").append(ready)
          .append(',')
          .append("\"allReady\":").append(result.allReady())
          .append(',')
          .append("\"gameStarted\":").append(session.isGameStarted())
          .append(',')
          .append("\"turn\":");
        MessageEncodingUtil.appendTurn(sb, result.firstTurn());
        sb.append('}');
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
}
