package teamnova.omok.decoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import teamnova.omok.message.decoder.HelloWorldDecoder;

class HelloWorldDecoderTest {

    @Test
    void decodesUtf8Payload() {
        HelloWorldDecoder decoder = new HelloWorldDecoder();
        String decoded = decoder.decode("world".getBytes());
        assertEquals("world", decoded);
    }

    @Test
    void handlesEmptyPayload() {
        HelloWorldDecoder decoder = new HelloWorldDecoder();
        String decoded = decoder.decode(new byte[0]);
        assertNotNull(decoded);
        assertEquals("", decoded);
    }
}
