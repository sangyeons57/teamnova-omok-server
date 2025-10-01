package teamnova.omok.handler.decoder;

import java.nio.charset.StandardCharsets;

public final class HelloWorldDecoder {
    public String decode(byte[] payload) {
        if (payload == null || payload.length == 0) {
            return "";
        }
        return new String(payload, StandardCharsets.UTF_8);
    }
}
