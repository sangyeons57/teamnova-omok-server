package teamnova.omok.codec.encoder;

import teamnova.omok.nio.ClientSession;

import java.nio.ByteBuffer;
import java.util.Objects;

public class EncodeFrame {
    public static ByteBuffer encodeFrame(byte type, long requestId, byte[] payload) {
        Objects.requireNonNull(payload, "payload");
        if (payload.length > ClientSession.maxPayloadSize()) {
            throw new IllegalArgumentException("payload length " + payload.length + " exceeds maximum " + ClientSession.maxPayloadSize());
        }

        int totalLength = ClientSession.headerLength() + payload.length;
        ByteBuffer buffer = ByteBuffer.allocate(totalLength);
        buffer.putInt(totalLength);
        buffer.put(type);
        buffer.putInt((int) (requestId & 0xFFFF_FFFFL));
        buffer.put(payload);
        buffer.flip();
        return buffer;
    }
}

