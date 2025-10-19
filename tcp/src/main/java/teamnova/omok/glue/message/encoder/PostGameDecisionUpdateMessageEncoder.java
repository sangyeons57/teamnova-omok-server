package teamnova.omok.glue.message.encoder;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import teamnova.omok.glue.game.session.model.PostGameDecision;
import teamnova.omok.glue.game.session.model.messages.PostGameDecisionUpdate;

public final class PostGameDecisionUpdateMessageEncoder {
    private PostGameDecisionUpdateMessageEncoder() {}

    public static byte[] encode(PostGameDecisionUpdate update) {
        StringBuilder sb = new StringBuilder(256);
        sb.append('{')
          .append("\"sessionId\":\"").append(update.session().sessionId().asUuid()).append('\"')
          .append(',')
          .append("\"decisions\":[");
        Map<String, PostGameDecision> decisions = update.decisions();
        boolean first = true;
        for (Map.Entry<String, PostGameDecision> entry : decisions.entrySet()) {
            if (!first) {
                sb.append(',');
            }
            first = false;
            sb.append('{')
              .append("\"userId\":\"").append(MessageEncodingUtil.escape(entry.getKey())).append('\"')
              .append(',')
              .append("\"decision\":\"").append(entry.getValue().name()).append('\"')
              .append('}');
        }
        sb.append(']')
          .append(',')
          .append("\"remaining\":[");
        List<String> remaining = update.remaining();
        for (int i = 0; i < remaining.size(); i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append('\"').append(MessageEncodingUtil.escape(remaining.get(i))).append('\"');
        }
        sb.append(']')
          .append('}');
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
}
