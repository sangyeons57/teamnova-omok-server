package teamnova.omok.tcp.nio;

import java.io.Closeable;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Arrays;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import teamnova.omok.tcp.nio.util.ByteArrayReaders;

/**
 * Represents a single client connection managed by the selector.
 */
public final class ClientSession implements Closeable {
    private static final int BUFFER_SIZE = 4096;
    private static final int LENGTH_FIELD_SIZE = Integer.BYTES; // length측정 은 4바이트 사용
    private static final int TYPE_FIELD_SIZE = 1;    // type식별은 0~255 1바이트 사용
    private static final int REQUEST_ID_FIELD_SIZE = Integer.BYTES; // request id는 4바이트  unsigned_int(long) 사용
    private static final int HEADER_LENGTH = LENGTH_FIELD_SIZE + TYPE_FIELD_SIZE + REQUEST_ID_FIELD_SIZE;
    private static final int MAX_PAYLOAD_SIZE = 1 << 20; // 1 MiB safety cap , 1<<30 하면 1GiB 로 cap올리기 가능
    private static final int MAX_FRAME_SIZE = HEADER_LENGTH + MAX_PAYLOAD_SIZE;

    private final SocketChannel channel;
    private final ByteBuffer readBuffer = ByteBuffer.allocate(BUFFER_SIZE);
    private final Queue<ByteBuffer> outbound = new ConcurrentLinkedQueue<>();
    private byte[] inboundBuffer = new byte[BUFFER_SIZE];
    private int inboundSize = 0;
    private SelectionKey key;

    public ClientSession(SocketChannel channel) {
        this.channel = Objects.requireNonNull(channel, "channel");
    }

    void attachKey(SelectionKey key) {
        this.key = key;
    }

    int readFromChannel() throws IOException {
        int bytesRead = channel.read(readBuffer);
        if (bytesRead > 0) {
            readBuffer.flip();
            int remaining = readBuffer.remaining();
            ensureCapacity(inboundSize + remaining);
            readBuffer.get(inboundBuffer, inboundSize, remaining);
            inboundSize += remaining;
            readBuffer.clear();
        }
        return bytesRead;
    }

    FramedMessage pollInboundFrame() throws PayloadTooLargeException {
        if (inboundSize < HEADER_LENGTH) {
            return null;
        }

        int totalLength = ByteArrayReaders.readIntBE(inboundBuffer, 0, inboundSize);
        if (totalLength < HEADER_LENGTH) {
            throw new PayloadTooLargeException("Frame length " + totalLength + " smaller than header");
        }
        if (totalLength > MAX_FRAME_SIZE) {
            throw new PayloadTooLargeException("Frame length " + totalLength + " exceeds maximum " + MAX_FRAME_SIZE);
        }

        if (inboundSize < totalLength) {
            return null;
        }

        byte type = inboundBuffer[LENGTH_FIELD_SIZE];
        long requestId = ByteArrayReaders.readUnsignedIntBE(inboundBuffer, LENGTH_FIELD_SIZE + TYPE_FIELD_SIZE, inboundSize);

        int payloadLength = totalLength - HEADER_LENGTH;
        byte[] payload = new byte[payloadLength];
        if (payloadLength > 0) {
            System.arraycopy(inboundBuffer, HEADER_LENGTH, payload, 0, payloadLength);
        }

        int remaining = inboundSize - totalLength;
        if (remaining > 0) {
            System.arraycopy(inboundBuffer, totalLength, inboundBuffer, 0, remaining);
        }
        inboundSize = remaining;

        return new FramedMessage(type, requestId, payload);
    }

    public void enqueueResponse(byte type, long requestId, byte[] payload) {
        outbound.add(NioReactorServer.encodeFrame(type, requestId, payload));
    }

    void flushOutbound() throws IOException {
        ByteBuffer buffer = outbound.peek();
        while (buffer != null) {
            channel.write(buffer);
            if (buffer.hasRemaining()) {
                return;
            }
            outbound.poll();
            buffer = outbound.peek();
        }
    }

    boolean hasPendingWrites() {
        return !outbound.isEmpty();
    }

    public void enableWriteInterest() {
        if (key != null && key.isValid()) {
            key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
        }
    }

    void disableWriteInterest() {
        if (key != null && key.isValid()) {
            key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
        }
    }

    void resetInboundState() {
        inboundSize = 0;
    }

    SocketAddress remoteAddress() throws IOException {
        return channel.getRemoteAddress();
    }

    @Override
    public void close() throws IOException {
        if (key != null) {
            key.cancel();
        }
        channel.close();
    }

    private void ensureCapacity(int required) {
        if (required <= inboundBuffer.length) {
            return;
        }
        int newCapacity = inboundBuffer.length;
        while (newCapacity < required) {
            newCapacity <<= 1;
        }
        inboundBuffer = Arrays.copyOf(inboundBuffer, newCapacity);
    }

    static final class PayloadTooLargeException extends Exception {
        PayloadTooLargeException(String message) {
            super(message);
        }
    }

    static int maxPayloadSize() {
        return MAX_PAYLOAD_SIZE;
    }

    static int headerLength() {
        return HEADER_LENGTH;
    }
}
