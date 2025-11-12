package teamnova.omok.core.nio;

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

import teamnova.omok.core.nio.codec.DecodeFrame;
import teamnova.omok.core.nio.codec.EncodeFrame;
import teamnova.omok.glue.handler.register.Type;

/**
 * Low-level transport wrapper around a non-blocking {@link SocketChannel}.
 * Handles framed message buffering and selector interest bookkeeping.
 */
public final class NioClientConnection implements Closeable {
    private static final int BUFFER_SIZE = 4096;
    private static final long IDLE_TIMEOUT_MILLIS = 90_000L;

    private final SocketChannel channel;
    private final ByteBuffer readBuffer = ByteBuffer.allocate(BUFFER_SIZE);
    private final Queue<ByteBuffer> outbound = new ConcurrentLinkedQueue<>();

    private byte[] inboundBuffer = new byte[BUFFER_SIZE];
    private int inboundSize = 0;
    private SelectionKey key;
    private long lastContactTime;

    public NioClientConnection(SocketChannel channel) {
        this.channel = Objects.requireNonNull(channel, "channel");
        this.lastContactTime = System.currentTimeMillis();
    }

    public void attachKey(SelectionKey key) {
        this.key = key;
    }

    public int readFromChannel() throws IOException {
        int bytesRead = channel.read(readBuffer);
        if (bytesRead > 0) {
            readBuffer.flip();
            int remaining = readBuffer.remaining();
            ensureCapacity(inboundSize + remaining);
            readBuffer.get(inboundBuffer, inboundSize, remaining);
            inboundSize += remaining;
            readBuffer.clear();
            updateLastContactTime();
        }
        return bytesRead;
    }

    public FramedMessage pollInboundFrame() throws DecodeFrame.FrameDecodeException {
        DecodeFrame.Result result = DecodeFrame.tryDecode(inboundBuffer, inboundSize);
        if (result == null) {
            return null;
        }
        int consumed = result.bytesConsumed();
        int remaining = inboundSize - consumed;
        if (remaining > 0) {
            System.arraycopy(inboundBuffer, consumed, inboundBuffer, 0, remaining);
        }
        inboundSize = remaining;
        return result.frame();
    }

    public void enqueueResponse(Type type, long requestId, byte[] payload) {
        outbound.add(EncodeFrame.encodeFrame(type.value, requestId, payload));
        updateLastContactTime();
    }

    public void flushOutbound() throws IOException {
        ByteBuffer buffer = outbound.peek();
        while (buffer != null) {
            channel.write(buffer);
            if (buffer.hasRemaining()) {
                return;
            }
            outbound.poll();
            buffer = outbound.peek();
        }
        updateLastContactTime();
    }

    public boolean hasPendingWrites() {
        return !outbound.isEmpty();
    }

    public void enableWriteInterest() {
        if (key != null && key.isValid()) {
            key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
        }
    }

    public void disableWriteInterest() {
        if (key != null && key.isValid()) {
            key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
        }
    }

    public void resetInboundState() {
        inboundSize = 0;
    }

    public SocketAddress remoteAddress() throws IOException {
        return channel.getRemoteAddress();
    }

    public void updateLastContactTime() {
        lastContactTime = System.currentTimeMillis();
    }

    public boolean isTimedOut(long nowMillis) {
        return (nowMillis - lastContactTime) >= IDLE_TIMEOUT_MILLIS;
    }

    @Override
    public void close() {
        try {
            if (key != null) {
                key.cancel();
            }
            channel.close();
        } catch (IOException e) {
            System.err.println("Connection close failure: " + e.getMessage());
        }
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
}
