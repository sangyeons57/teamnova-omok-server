package teamnova.omok.glue.message.encoder;

import java.nio.charset.StandardCharsets;
import java.util.List;
import teamnova.omok.glue.store.GameSession;

public final class JoinSessionMessageEncoder {
    private JoinSessionMessageEncoder() {}

    public static byte[] encode(GameSession session) {
        StringBuilder sb = new StringBuilder(256);
        sb.append('{')
          .append("\"sessionId\":\"").append(session.getId()).append('\"')
          .append(',')
          .append("\"createdAt\":").append(session.getCreatedAt())
          .append(',')
          .append("\"users\":[");
        List<String> userIds = session.getUserIds();
        for (int i = 0; i < userIds.size(); i++) {
            String uid = userIds.get(i);
            sb.append('{')
              .append("\"userId\":\"").append(MessageEncodingUtil.escape(uid)).append('\"')
              .append(',')
              .append("\"displayName\":\"").append(MessageEncodingUtil.escape(uid)).append('\"')
              .append(',')
              .append("\"profileIconCode\":").append(0)
              .append('}');
            if (i < userIds.size() - 1) {
                sb.append(',');
            }
        }
        sb.append(']').append('}');
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
}
