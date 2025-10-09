package teamnova.omok.message.decoder;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * Test decoder converting ASCII uppercase payload to lowercase.
 */
public class HelloWorldDecoder {
    public String decode(ByteBuffer buffer) {
        if (buffer == null) {
            return "";
        }
        byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        return decode(bytes);
    }

    public String decode(byte[] payload) {
        if (payload == null || payload.length == 0) {
            return "";
        }
        return new String(payload, StandardCharsets.UTF_8).toLowerCase();
    }
}
