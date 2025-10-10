package teamnova.omok.message.encoder;

import java.nio.charset.StandardCharsets;

import teamnova.omok.service.InGameSessionService;

public final class PostGameDecisionAckMessageEncoder {
    private PostGameDecisionAckMessageEncoder() {}

    public static byte[] encode(InGameSessionService.PostGameDecisionResult result) {
        StringBuilder sb = new StringBuilder(128);
        sb.append('{')
          .append("\"status\":\"");
        if (result.status() == InGameSessionService.PostGameDecisionStatus.ACCEPTED) {
            sb.append("OK\"");
            if (result.decision() != null) {
                sb.append(",\"decision\":\"").append(result.decision().name()).append('\"');
            }
        } else {
            sb.append("ERROR\",\"reason\":\"").append(result.status().name()).append('\"');
        }
        sb.append('}');
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
}
