package teamnova.omok.message.decoder;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Utility decoder that turns UTF-8 payloads into Strings.
 */
public class StringDecoder {
    public String decode(ByteBuffer buffer) {
        if (buffer == null) {
            return "";
        }
        byte[] data = new byte[buffer.remaining()];
        buffer.get(data);
        return decode(data);
    }

    public String decode(byte[] payload) {
        if (payload == null || payload.length == 0) {
            return "";
        }
        return new String(payload, StandardCharsets.UTF_8);
    }
}
