package teamnova.omok.glue.client.session.services;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import teamnova.omok.core.nio.FramedMessage;
import teamnova.omok.core.nio.NioClientConnection;
import teamnova.omok.core.nio.NioReactorServer;
import teamnova.omok.core.nio.codec.DecodeFrame;
import teamnova.omok.glue.game.session.model.PlayerResult;
import teamnova.omok.glue.game.session.model.vo.GameSessionId;
import teamnova.omok.glue.handler.register.Type;
import teamnova.omok.glue.client.session.interfaces.ClientSessionHandle;
import teamnova.omok.glue.client.session.interfaces.ClientSessionLifecycleListener;
import teamnova.omok.glue.client.session.model.ClientSession;
import teamnova.omok.glue.client.session.states.ClientStateHub;

/**
 * Glue-layer view of a connected client: combines transport, authentication state,
 * and client-level state machine orchestration.
 */
public final class ManagedClientSession implements ClientSessionHandle {
    private final NioClientConnection connection;
    private final NioReactorServer server;
    private final ClientSessionLifecycleListener lifecycleListener;
    private final ClientSession model = new ClientSession();
    private final ClientStateHub stateHub;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public ManagedClientSession(NioClientConnection connection,
                                NioReactorServer server,
                                ClientSessionLifecycleListener lifecycleListener) {
        this.connection = Objects.requireNonNull(connection, "connection");
        this.server = Objects.requireNonNull(server, "server");
        this.lifecycleListener = Objects.requireNonNull(lifecycleListener, "lifecycleListener");
        this.stateHub = new ClientStateHub(this);
    }

    @Override
    public void attachKey(SelectionKey key) {
        connection.attachKey(key);
    }

    @Override
    public int readFromChannel() throws IOException {
        return connection.readFromChannel();
    }

    @Override
    public FramedMessage pollInboundFrame() throws DecodeFrame.FrameDecodeException {
        return connection.pollInboundFrame();
    }

    @Override
    public void enqueueResponse(Type type, long requestId, byte[] payload) {
        connection.enqueueResponse(type, requestId, payload);
        server.enqueueSelectorTask(connection::enableWriteInterest);
    }

    @Override
    public void flushOutbound() throws IOException {
        connection.flushOutbound();
    }

    @Override
    public boolean hasPendingWrites() {
        return connection.hasPendingWrites();
    }

    @Override
    public void enableWriteInterest() {
        connection.enableWriteInterest();
    }

    @Override
    public void disableWriteInterest() {
        connection.disableWriteInterest();
    }

    @Override
    public void resetInboundState() {
        connection.resetInboundState();
    }

    @Override
    public SocketAddress remoteAddress() throws IOException {
        return connection.remoteAddress();
    }

    @Override
    public void closeIfTimedOut(long nowMillis) {
        if (connection.isTimedOut(nowMillis)) {
            close();
        }
    }

    @Override
    public void processLifecycle(long now) {
        stateHub.process(now);
    }

    @Override
    public ClientSession model() {
        return model;
    }

    @Override
    public void bindGameSession(teamnova.omok.glue.game.session.model.vo.GameSessionId id) {
        model.bindGameSession(id);
    }

    @Override
    public void unbindGameSession(teamnova.omok.glue.game.session.model.vo.GameSessionId id) {
        model.unbindGameSession(id);
    }

    @Override
    public GameSessionId currentGameSessionId() {
        return model.currentGameSessionId();
    }

    ClientStateHub stateHub() {
        return stateHub;
    }

    @Override
    public boolean isAuthenticated() {
        return model.isAuthenticated();
    }

    @Override
    public String authenticatedUserId() {
        return model.userId();
    }

    @Override
    public void markAuthenticated(String userId, String role, String scope) {
        model.markAuthenticated(userId, role, scope);
        stateHub.markAuthenticated();
    }

    @Override
    public void clearAuthentication() {
        model.clearAuthentication();
        stateHub.resetToConnected();
    }

    @Override
    public ClientSession.ClientSessionMetrics registerOutcome(PlayerResult result) {
        model.registerOutcome(result);
        return model.metricsSnapshot();
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) {
            return;
        }
        lifecycleListener.onSessionClosed(this);
        stateHub.disconnect();
        connection.close();
    }
}
