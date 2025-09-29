package teamnova.omok.tcp;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Logger;

/**
 * NIO selector/reactor server that delegates business logic to worker threads.
 */
public class NioReactorServer implements Closeable {
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
                        // Key cancelled during processing; ignore.
                        System.err.println("Key cancelled during processing: " + ignored.getMessage());
                        continue;
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
            String message;
            while ((message = session.pollInboundMessage()) != null) {
                dispatcher.submit(new MessageEvent(session, message));
            }
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

    void enqueueSelectorTask(Runnable task) {
        Objects.requireNonNull(task, "task");
        selectorTasks.add(task);
        selector.wakeup();
    }

    void closeSession(ClientSession session) {
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

    static ByteBuffer encode(String value) {
        return ByteBuffer.wrap(value.getBytes(StandardCharsets.UTF_8));
    }
}
