package teamnova.omok.glue.client.session.interfaces.transport;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;

import teamnova.omok.core.nio.FramedMessage;
import teamnova.omok.core.nio.codec.DecodeFrame;

/**
 * Low-level transport operations needed by the NIO reactor loop.
 */
public interface ManagedSessionTransport {
    void attachKey(SelectionKey key);

    int readFromChannel() throws IOException;

    FramedMessage pollInboundFrame() throws DecodeFrame.FrameDecodeException;

    void flushOutbound() throws IOException;

    boolean hasPendingWrites();

    void enableWriteInterest();

    void disableWriteInterest();

    void resetInboundState();

    SocketAddress remoteAddress() throws IOException;

    void closeIfTimedOut(long nowMillis);

    void processLifecycle(long now);

    void close();
}
