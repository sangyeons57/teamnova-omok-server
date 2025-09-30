package teamnova.omok.codec.encoder;

import teamnova.omok.codec.FrameFormat;

import java.nio.ByteBuffer;
import java.util.Objects;

public class EncodeFrame {
    public static ByteBuffer encodeFrame(byte type, long requestId, byte[] payload) {
        Objects.requireNonNull(payload, "payload");
        if (payload.length > FrameFormat.MAX_PAYLOAD_SIZE) {
            throw new IllegalArgumentException("payload length " + payload.length + " exceeds maximum " + FrameFormat.MAX_PAYLOAD_SIZE);
        }

        int totalLength = FrameFormat.HEADER_LENGTH + payload.length;
        ByteBuffer buffer = ByteBuffer.allocate(totalLength);
        buffer.putInt(totalLength);
        buffer.put(type);
        buffer.putInt((int) (requestId & 0xFFFF_FFFFL));
        buffer.put(payload);
        buffer.flip();
        return buffer;
    }
}

