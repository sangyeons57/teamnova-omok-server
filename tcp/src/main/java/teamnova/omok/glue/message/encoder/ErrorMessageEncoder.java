package teamnova.omok.glue.message.encoder;

import java.nio.charset.StandardCharsets;

public final class ErrorMessageEncoder {
    private ErrorMessageEncoder() {}

    public static byte[] encode(String reason) {
        StringBuilder sb = new StringBuilder(96);
        sb.append('{')
          .append("\"ok\":false,")
          .append("\"reason\":\"").append(MessageEncodingUtil.escape(reason)).append('\"')
          .append('}');
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
}
