package teamnova.omok.tcp;

import java.io.Closeable;
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Represents a single client connection managed by the selector.
 */
final class ClientSession implements Closeable {
    private static final int BUFFER_SIZE = 4096;

    private final SocketChannel channel;
    private final ByteBuffer readBuffer = ByteBuffer.allocate(BUFFER_SIZE);
    private final Queue<ByteBuffer> outbound = new ConcurrentLinkedQueue<>();
    private final StringBuilder inbound = new StringBuilder();
    private SelectionKey key;

    ClientSession(SocketChannel channel) {
        this.channel = Objects.requireNonNull(channel, "channel");
    }

    void attachKey(SelectionKey key) {
        this.key = key;
    }

    int readFromChannel() throws IOException {
        int bytesRead = channel.read(readBuffer);
        if (bytesRead > 0) {
            readBuffer.flip();
            byte[] data = new byte[readBuffer.remaining()];
            readBuffer.get(data);
            readBuffer.clear();
            inbound.append(new String(data, StandardCharsets.UTF_8));
        }
        return bytesRead;
    }

    String pollInboundMessage() {
        int newline = inbound.indexOf("\n");
        if (newline == -1) {
            return null;
        }
        String message = inbound.substring(0, newline);
        inbound.delete(0, newline + 1);
        return message.replace("\r", "");
    }

    void enqueueResponse(String message) {
        outbound.add(NioReactorServer.encode(message));
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

    void enableWriteInterest() {
        if (key != null && key.isValid()) {
            key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);
        }
    }

    void disableWriteInterest() {
        if (key != null && key.isValid()) {
            key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);
        }
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
}
