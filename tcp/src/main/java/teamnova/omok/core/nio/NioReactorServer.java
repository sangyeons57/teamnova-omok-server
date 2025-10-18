package teamnova.omok.core.nio;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import teamnova.omok.core.nio.codec.DecodeFrame;
import teamnova.omok.glue.handler.dispatcher.Dispatcher;
import teamnova.omok.glue.handler.register.HandlerRegistry;
import teamnova.omok.glue.client.session.ClientSessionManager;
import teamnova.omok.glue.client.session.interfaces.ClientSessionHandle;

/**
 * NIO selector/reactor server that delegates business logic to worker threads.
 */
public final class NioReactorServer implements Closeable {
    private final Selector selector;
    private final ServerSocketChannel serverChannel;
    private final Dispatcher dispatcher;
    private final Queue<Runnable> selectorTasks = new ConcurrentLinkedQueue<>();
    private volatile boolean running = true;

    public NioReactorServer(int port, int workerCount, HandlerRegistry registry) throws IOException {
        this.selector = Selector.open();
        this.serverChannel = ServerSocketChannel.open();
        this.serverChannel.configureBlocking(false);
        this.serverChannel.bind(new InetSocketAddress(port));
        this.serverChannel.register(selector, SelectionKey.OP_ACCEPT);
        this.dispatcher = new Dispatcher(workerCount, this);
        Objects.requireNonNull(registry, "registry").configure(dispatcher);
    }

    public void start() {
        try {
            while (running) {
                // Use a timeout to periodically wake up and perform housekeeping (e.g., idle session checks)
                selector.select(1000);
                runSelectorTasks();

                // Sweep for idle sessions and close them
                long now = System.currentTimeMillis();
                for (SelectionKey k : selector.keys()) {
                    Object att = k.attachment();
                    if (att instanceof ClientSessionHandle s) {
                        s.closeIfTimedOut(now);
                    }
                }

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
        NioClientConnection connection = new NioClientConnection(client);
        ClientSessionHandle session = ClientSessionManager.getInstance().registerConnection(connection, this);
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
        ClientSessionHandle session = (ClientSessionHandle) key.attachment();
        if (session == null) {
            return;
        }
        try {
            int bytesRead = session.readFromChannel();
            if (bytesRead == -1) {
                session.close();
                return;
            }
            FramedMessage frame;
            while ((frame = session.pollInboundFrame()) != null) {
                dispatcher.dispatch(session, frame);
            }
        } catch (DecodeFrame.FrameDecodeException e) {
            System.err.println("Frame discarded: " + e.getMessage());
            session.resetInboundState();
            session.close();
        } catch (IOException e) {
            System.err.println("Read failure: " + e.getMessage());
            session.close();
        }
    }

    private void handleWrite(SelectionKey key) {
        ClientSessionHandle session = (ClientSessionHandle) key.attachment();
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
            session.close();
        }
    }

    public void enqueueSelectorTask(Runnable task) {
        Objects.requireNonNull(task, "task");
        selectorTasks.add(task);
        selector.wakeup();
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
                if (attachment instanceof ClientSessionHandle session) {
                    session.close();
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

}
