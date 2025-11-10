package teamnova.omok.glue.client.session.services;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.channels.SelectionKey;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import teamnova.omok.core.nio.FramedMessage;
import teamnova.omok.core.nio.NioClientConnection;
import teamnova.omok.core.nio.NioReactorServer;
import teamnova.omok.core.nio.codec.DecodeFrame;
import teamnova.omok.glue.client.session.ClientSessionManager;
import teamnova.omok.glue.client.session.interfaces.ClientSessionHandle;
import teamnova.omok.glue.client.session.interfaces.ClientSessionStateListener;
import teamnova.omok.glue.client.session.model.ClientSession;
import teamnova.omok.glue.client.state.ClientStateCommandBus;
import teamnova.omok.glue.client.state.ClientStateHub;
import teamnova.omok.glue.game.session.GameSessionManager;
import teamnova.omok.glue.game.session.model.PlayerResult;
import teamnova.omok.glue.game.session.model.PostGameDecision;
import teamnova.omok.glue.game.session.model.vo.GameSessionId;
import teamnova.omok.glue.handler.register.Type;

/**
 * Glue-layer view of a connected client: combines transport, authentication state,
 * and client-level state machine orchestration.
 */
public final class ManagedClientSession implements ClientSessionHandle {
    private final NioClientConnection connection;
    private final NioReactorServer server;
    private final ClientSession model = new ClientSession();
    private final ClientStateHub stateHub;
    private final ClientStateCommandBus stateCommands;
    private final AtomicBoolean closed = new AtomicBoolean(false);

    public ManagedClientSession(NioClientConnection connection,
                                NioReactorServer server,
                                ClientSessionStore store) {
        this.connection = Objects.requireNonNull(connection, "connection");
        this.server = Objects.requireNonNull(server, "server");
        this.stateHub = new ClientStateHub(this, Objects.requireNonNull(store, "store"));
        this.stateCommands = new ClientStateCommandBus(this.stateHub);
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

    @Override
    public void requestMatchmaking(long requestId, int rating, Set<Integer> matchSizes) {
        stateCommands.requestMatchmaking(requestId, rating, matchSizes);
    }

    @Override
    public void cancelMatchmaking(long requestId) {
        stateCommands.cancelMatchmaking(requestId);
    }

    @Override
    public void authenticateUser(String userId, String role, String scope) {
        ClientSessionManager.getInstance().onAuthenticated(this, userId, role, scope);
    }

    @Override
    public void clearAuthenticationBinding() {
        ClientSessionManager.getInstance().onAuthenticationCleared(this);
    }

    @Override
    public void sendAuthResult(long requestId, boolean success) {
        ClientSessionManager.getInstance()
            .clientPublisher(this)
            .authResult(requestId, success);
    }

    @Override
    public void sendHello(long requestId, String response) {
        ClientSessionManager.getInstance()
            .clientPublisher(this)
            .hello(requestId, response);
    }

    @Override
    public void sendPingPong(long requestId, byte[] payload) {
        ClientSessionManager.getInstance()
            .clientPublisher(this)
            .pingPong(requestId, payload);
    }

    @Override
    public void sendReconnectResult(long requestId, boolean success, String detail) {
        ClientSessionManager.getInstance()
            .clientPublisher(this)
            .reconnectResult(requestId, success, detail);
    }

    @Override
    public boolean submitMove(long requestId, int x, int y) {
        String userId = authenticatedUserId();
        if (userId == null) {
            return false;
        }
        return GameSessionManager.getInstance().submitMove(userId, requestId, x, y);
    }

    @Override
    public void sendPlaceStoneError(long requestId, String message) {
        ClientSessionManager.getInstance()
            .clientPublisher(this)
            .placeStoneError(requestId, message);
    }

    @Override
    public void submitReady(long requestId) {
        String userId = authenticatedUserId();
        if (userId != null) {
            GameSessionManager.getInstance().submitReady(userId, requestId);
        }
    }

    @Override
    public void leaveInGameSession(long requestId) {
        if (!isAuthenticated()) {
            return;
        }
        String userId = authenticatedUserId();
        if (userId == null) {
            return;
        }
        GameSessionManager gameManager = GameSessionManager.getInstance();
        ClientSessionManager clientManager = ClientSessionManager.getInstance();
        gameManager.findSession(userId).ifPresent(gs -> {
            for (String uid : gs.getUserIds()) {
                if (!uid.equals(userId)) {
                    clientManager.clientPublisher(uid)
                        .ifPresent(channel -> channel.notifyPeerLeft(userId));
                }
            }
        });
        gameManager.leaveSession(userId);
        clientManager.clientPublisher(this).leaveInGameAck(requestId);
    }

    @Override
    public void submitPostGameDecision(long requestId, PostGameDecision decision) {
        String userId = authenticatedUserId();
        if (userId != null && decision != null) {
            GameSessionManager.getInstance().submitPostGameDecision(userId, requestId, decision);
        }
    }

    @Override
    public boolean reconnectGameSession(GameSessionId sessionId) {
        String userId = authenticatedUserId();
        if (userId == null) {
            return false;
        }
        return GameSessionManager.getInstance().handleClientReconnected(userId, sessionId);
    }

    @Override
    public void beginReconnectFlow() {
        stateCommands.beginReconnect();
    }

    @Override
    public void finishReconnectFlow(boolean success) {
        stateCommands.finishReconnect(success);
    }

    ClientStateHub stateHub() {
        return stateHub;
    }

    public void addStateListener(ClientSessionStateListener listener) {
        stateHub.addStateListener(listener);
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
        stateCommands.markAuthenticated();
    }

    @Override
    public void clearAuthentication() {
        model.clearAuthentication();
        stateCommands.resetToConnected();
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
        stateCommands.disconnect();
        stateHub.drainPending();
        connection.close();
    }
}
