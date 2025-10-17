package teamnova.omok.glue.message.encoder;

import java.nio.charset.StandardCharsets;
import java.util.List;
import teamnova.omok.glue.store.GameSession;
import teamnova.omok.glue.data.model.UserData;

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
        List<UserData> userDataIds = session.getUsers();
        for (int i = 0; i < userDataIds.size(); i++) {
            UserData userData = userDataIds.get(i);
            sb.append('{')
              .append("\"userId\":\"").append(MessageEncodingUtil.escape(userData.id())).append('\"')
              .append(',')
              .append("\"displayName\":\"").append(MessageEncodingUtil.escape(userData.name())).append('\"')
              .append(',')
              .append("\"profileIconCode\":").append(userData.profileIconCode())
              .append('}');
            if (i < userDataIds.size() - 1) {
                sb.append(',');
            }
        }
        sb.append(']').append('}');
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
}
