package teamnova.omok.glue.message.encoder;

import java.nio.charset.StandardCharsets;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionAccess;
import teamnova.omok.glue.game.session.model.messages.PostGameDecisionPrompt;

public final class PostGameDecisionPromptMessageEncoder {
    private PostGameDecisionPromptMessageEncoder() {}

    public static byte[] encode(GameSessionAccess session, PostGameDecisionPrompt prompt) {
        StringBuilder sb = new StringBuilder(160);
        sb.append('{')
          .append("\"sessionId\":\"").append(session.sessionId().asUuid()).append('\"')
          .append(',')
          .append("\"deadlineAt\":").append(prompt.deadlineAt())
          .append(',')
          .append("\"options\":[\"REMATCH\",\"LEAVE\"],")
          .append("\"autoAction\":\"LEAVE\"")
          .append('}');
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
}
