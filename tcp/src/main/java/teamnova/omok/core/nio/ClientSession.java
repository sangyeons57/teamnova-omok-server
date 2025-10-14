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

import teamnova.omok.glue.handler.register.Type;
import teamnova.omok.core.nio.codec.DecodeFrame;
import teamnova.omok.core.nio.codec.EncodeFrame;
import teamnova.omok.glue.service.ServiceContainer;
import teamnova.omok.glue.state.client.ClientStateHub;

/**
 * Represents a single client connection managed by the selector.
 */
public final class ClientSession implements Closeable {
    private static final int BUFFER_SIZE = 4096;
    // Idle timeout in milliseconds. Can be made configurable if needed.
    private static final long IDLE_TIMEOUT_MILLIS = 60_000L;

    private final SocketChannel channel;
    private final ByteBuffer readBuffer = ByteBuffer.allocate(BUFFER_SIZE);
    private final Queue<ByteBuffer> outbound = new ConcurrentLinkedQueue<>();
    private byte[] inboundBuffer = new byte[BUFFER_SIZE];
    private int inboundSize = 0;
    private SelectionKey key;

    private long lastContactTime;

    private volatile boolean authenticated;
    private volatile String authenticatedUserId;
    private volatile String authenticatedRole;
    private volatile String authenticatedScope;

    private final ClientStateHub stateHub;


    public ClientSession(SocketChannel channel) {
        this.channel = Objects.requireNonNull(channel, "channel");
        // Initialize last contact time at session creation
        this.lastContactTime = System.currentTimeMillis();
        this.stateHub = new ClientStateHub(this);
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
            // Update last contact time on read activity
            updateLastContactTime();
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

    public void enqueueResponse(Type type, long requestId, byte[] payload) {
        outbound.add(EncodeFrame.encodeFrame(type.value, requestId, payload));
        // Update last contact time as we have outgoing data scheduled
        updateLastContactTime();
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
        // Update last contact time when all outbound data has been flushed
        updateLastContactTime();
    }

    boolean hasPendingWrites() {
        return !outbound.isEmpty();
    }

    void updateLastContactTime() {
        lastContactTime = System.currentTimeMillis();
    }

    // Check if this session has been idle for too long
    boolean isTimedOut(long nowMillis) {
        return (nowMillis - lastContactTime) >= IDLE_TIMEOUT_MILLIS;
    }

    // Close the session if the idle timeout has been exceeded
    void closeIfTimedOut(long nowMillis) {
        if (isTimedOut(nowMillis)) {
            // ensure matching ticket is canceled on idle timeout using helper
            ClientSessions.cancelMatchingIfAuthenticated(this);
            // remove from user session index if mapped to this session
            ClientSessions.onSessionClosed(this);
            close();
        }
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

    public SocketAddress remoteAddress() throws IOException {
        return channel.getRemoteAddress();
    }

    @Override
    public void close() {
        recordGameDisconnectIfNeeded();
        // cancel any outstanding matching ticket upon close
        ClientSessions.cancelMatchingIfAuthenticated(this);
        // remove from user session index if mapped to this session
        ClientSessions.onSessionClosed(this);
        stateHub.disconnect();
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

    public void markAuthenticated(String userId, String role, String scope) {
        this.authenticated = true;
        this.authenticatedUserId = userId;
        this.authenticatedRole = role;
        this.authenticatedScope = scope;
        stateHub.markAuthenticated();
    }

    public void clearAuthentication() {
        this.authenticated = false;
        this.authenticatedUserId = null;
        this.authenticatedRole = null;
        this.authenticatedScope = null;
        // revert to the connected state when authentication is cleared
        stateHub.resetToConnected();
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public String authenticatedUserId() {
        return authenticatedUserId;
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

    private void recordGameDisconnectIfNeeded() {
        if (!authenticated) {
            return;
        }
        String userId = authenticatedUserId;
        if (userId == null || userId.isBlank()) {
            return;
        }
        ServiceContainer.getInstance()
            .getInGameSessionService()
            .handleClientDisconnected(userId);
    }

}
