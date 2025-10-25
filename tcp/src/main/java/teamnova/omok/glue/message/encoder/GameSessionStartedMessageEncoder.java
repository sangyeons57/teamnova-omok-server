package teamnova.omok.glue.message.encoder;

import java.nio.charset.StandardCharsets;
import java.util.List;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionAccess;
import teamnova.omok.glue.game.session.model.GameSession;
import teamnova.omok.glue.rule.api.RuleId;

public final class GameSessionStartedMessageEncoder {
    private GameSessionStartedMessageEncoder() {}

    public static byte[] encode(GameSessionAccess session) {
        StringBuilder sb = new StringBuilder(320);
        sb.append('{')
          .append("\"sessionId\":\"").append(session.sessionId().asUuid()).append('\"')
          .append(',')
          .append("\"startedAt\":").append(session.getGameStartedAt())
          .append(',')
          .append("\"ruleIds\":");
        // Encode rule IDs as an array of enum names (UPPER_SNAKE_CASE)
        List<RuleId> ruleIds = session.getRuleIds();
        sb.append('[');
        if (ruleIds != null) {
            for (int i = 0; i < ruleIds.size(); i++) {
                if (i > 0) sb.append(',');
                sb.append('\"').append(ruleIds.get(i).name()).append('\"');
            }
        }
        sb.append(']')
          .append(',')
          .append("\"board\":{")
          .append("\"width\":").append(GameSession.BOARD_WIDTH).append(',')
          .append("\"height\":").append(GameSession.BOARD_HEIGHT)
          .append('}')
          .append('}');
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
}
