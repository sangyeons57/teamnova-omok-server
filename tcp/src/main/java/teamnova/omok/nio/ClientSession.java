package teamnova.omok.nio;

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

import teamnova.omok.codec.encoder.EncodeFrame;
import teamnova.omok.codec.decoder.DecodeFrame;

/**
 * Represents a single client connection managed by the selector.
 */
public final class ClientSession implements Closeable {
    private static final int BUFFER_SIZE = 4096;

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

    FramedMessage pollInboundFrame() throws DecodeFrame.FrameDecodeException {
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

    public void enqueueResponse(byte type, long requestId, byte[] payload) {
        outbound.add(EncodeFrame.encodeFrame(type, requestId, payload));
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
    public void close() {
        try {
            System.out.printf("Closing connection %s%n", remoteAddress());
        } catch (IOException ignore) { /* ignore */ }
        try {
            if (key != null) {
                key.cancel();
            }
            channel.close();
        } catch (IOException e) {
            System.err.println("Session close failure: " + e.getMessage());
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
