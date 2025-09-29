package teamnova.omok.nio;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import teamnova.omok.decoder.HelloWorldDecoder;
import teamnova.omok.dispatcher.Dispatcher;
import teamnova.omok.handler.HandlerProvider;
import teamnova.omok.handler.HelloWorldHandler;

/**
 * NIO selector/reactor server that delegates business logic to worker threads.
 */
public final class NioReactorServer implements Closeable {
    private final Selector selector;
    private final ServerSocketChannel serverChannel;
    private final Dispatcher dispatcher;
    private final Queue<Runnable> selectorTasks = new ConcurrentLinkedQueue<>();
    private volatile boolean running = true;

    public NioReactorServer(int port, int workerCount) throws IOException {
        this.selector = Selector.open();
        this.serverChannel = ServerSocketChannel.open();
        this.serverChannel.configureBlocking(false);
        this.serverChannel.bind(new InetSocketAddress(port));
        this.serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        this.dispatcher = new Dispatcher(workerCount, this);
        registerDefaultHandlers();
    }

    private void registerDefaultHandlers() {
        dispatcher.register(0, HandlerProvider.singleton(new HelloWorldHandler(new HelloWorldDecoder())));
    }

    public void start() {
        try {
            while (running) {
                selector.select();
                runSelectorTasks();
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();
                    if (!key.isValid()) {
                        continue;
                    }
                    try {
                        dispatchKey(key);
                    } catch (CancelledKeyException ignored) {
                        System.err.println("Key cancelled during processing: " + ignored.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Selector loop halted: " + e.getMessage());
        } finally {
            shutdownInternal();
        }
    }

    private void dispatchKey(SelectionKey key) throws IOException {
        if (key.isAcceptable()) {
            handleAccept(key);
        }
        if (key.isConnectable()) {
            handleConnect(key);
        }
        if (key.isReadable()) {
            handleRead(key);
        }
        if (key.isWritable()) {
            handleWrite(key);
        }
    }

    private void handleAccept(SelectionKey key) throws IOException {
        ServerSocketChannel server = (ServerSocketChannel) key.channel();
        SocketChannel client = server.accept();
        if (client == null) {
            return;
        }
        client.configureBlocking(false);
        ClientSession session = new ClientSession(client);
        SelectionKey clientKey = client.register(selector, SelectionKey.OP_READ, session);
        session.attachKey(clientKey);
        try {
            System.out.printf("Accepted connection from %s%n", session.remoteAddress());
        } catch (IOException e) {
            System.err.println("Failed to read remote address: " + e.getMessage());
        }
    }

    private void handleConnect(SelectionKey key) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        if (channel.isConnectionPending()) {
            channel.finishConnect();
        }
        key.interestOps(key.interestOps() & ~SelectionKey.OP_CONNECT | SelectionKey.OP_READ);
    }

    private void handleRead(SelectionKey key) {
        ClientSession session = (ClientSession) key.attachment();
        if (session == null) {
            return;
        }
        try {
            int bytesRead = session.readFromChannel();
            if (bytesRead == -1) {
                closeSession(session);
                return;
            }
            FramedMessage frame;
            while ((frame = session.pollInboundFrame()) != null) {
                dispatcher.dispatch(session, frame);
            }
        } catch (ClientSession.PayloadTooLargeException e) {
            System.err.println("Payload discarded: " + e.getMessage());
            session.resetInboundState();
            closeSession(session);
        } catch (IOException e) {
            System.err.println("Read failure: " + e.getMessage());
            closeSession(session);
        }
    }

    private void handleWrite(SelectionKey key) {
        ClientSession session = (ClientSession) key.attachment();
        if (session == null) {
            return;
        }
        try {
            session.flushOutbound();
            if (!session.hasPendingWrites()) {
                session.disableWriteInterest();
            }
        } catch (IOException e) {
            System.err.println("Write failure: " + e.getMessage());
            closeSession(session);
        }
    }

    public void enqueueSelectorTask(Runnable task) {
        Objects.requireNonNull(task, "task");
        selectorTasks.add(task);
        selector.wakeup();
    }

    public void closeSession(ClientSession session) {
        try {
            System.out.printf("Closing connection %s%n", session.remoteAddress());
        } catch (IOException ignore) {
            // ignore
        }
        try {
            session.close();
        } catch (IOException e) {
            System.err.println("Session close failure: " + e.getMessage());
        }
    }

    @Override
    public void close() {
        running = false;
        selector.wakeup();
    }

    private void shutdownInternal() {
        dispatcher.close();
        try {
            for (SelectionKey key : selector.keys()) {
                Object attachment = key.attachment();
                if (attachment instanceof ClientSession session) {
                    try {
                        session.close();
                    } catch (IOException ignore) {
                        // Ignore on shutdown.
                    }
                }
                key.cancel();
            }
            selector.close();
            serverChannel.close();
        } catch (IOException e) {
            System.err.println("Failed to shutdown cleanly: " + e.getMessage());
        }
    }

    private void runSelectorTasks() {
        Runnable task;
        while ((task = selectorTasks.poll()) != null) {
            try {
                task.run();
            } catch (RuntimeException e) {
                System.err.println("Selector task failed: " + e.getMessage());
            }
        }
    }

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
