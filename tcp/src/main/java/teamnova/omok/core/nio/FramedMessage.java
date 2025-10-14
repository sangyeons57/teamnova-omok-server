package teamnova.omok.core.nio;

/**
 * Represents a single binary frame decoded from the wire.
 */
public final class FramedMessage {
    private final byte type;
    private final long requestId;
    private final byte[] payload;

    public FramedMessage(byte type, long requestId, byte[] payload) {
        this.type = type;
        this.requestId = requestId;
        this.payload = payload;
    }

    public byte type() {
        return type;
    }

    public long requestId() {
        return requestId;
    }

    public byte[] payload() {
        return payload;
    }

    @Override
    public String toString() {
        return "FramedMessage{" +
                "type=" + (type & 0xFF) +
                ", requestId=" + requestId +
                ", payloadSize=" + payload.length +
                '}';
    }
}
