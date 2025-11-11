package teamnova.omok.glue.client.session.services;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Set;

import teamnova.omok.glue.client.session.interfaces.ClientSessionHandle;
import teamnova.omok.glue.handler.register.Type;
import teamnova.omok.glue.message.encoder.ErrorMessageEncoder;
import teamnova.omok.glue.client.session.model.AuthResultStatus;

/**
 * Provides reusable helpers for sending client-scoped messages with predefined message shapes.
 */
public final class ClientSessionMessagePublisher {
    private static final byte[] SESSION_REPLACED_BYTES = "SESSION_REPLACED".getBytes(StandardCharsets.UTF_8);
    private static final byte[] AUTH_SUCCESS_BYTES = "1".getBytes(StandardCharsets.UTF_8);
    private static final byte[] AUTH_FAILURE_BYTES = "0".getBytes(StandardCharsets.UTF_8);
    private static final byte[] AUTH_RECONNECTED_BYTES = "2".getBytes(StandardCharsets.UTF_8);
    private static final byte[] LEAVE_MATCH_BYTES = "CANCELLED".getBytes(StandardCharsets.UTF_8);
    private static final byte[] LEAVE_GAME_BYTES = "LEFT".getBytes(StandardCharsets.UTF_8);


    public ClientSessionMessagePublisher() {
    }

    public SessionChannel session(ClientSessionHandle handle) {
        return new SessionChannel(handle);
    }

    public final class SessionChannel {
        private final ClientSessionHandle handle;

        private SessionChannel(ClientSessionHandle handle) {
            this.handle = Objects.requireNonNull(handle, "handle");
        }

        public void notifyReplaced() {
            send(Type.ERROR, 0L, SESSION_REPLACED_BYTES);
        }

        public void authResult(long requestId, AuthResultStatus status) {
            byte[] payload = switch (status) {
                case SUCCESS -> AUTH_SUCCESS_BYTES;
                case FAILURE -> AUTH_FAILURE_BYTES;
                case RECONNECTED -> AUTH_RECONNECTED_BYTES;
            };
            send(Type.AUTH, requestId, payload);
        }

        public void hello(long requestId, String response) {
            send(Type.HELLO, requestId, encodeUtf8(response));
        }

        public void pingPong(long requestId, byte[] payload) {
            send(Type.PINGPONG, requestId, payload == null ? new byte[0] : payload);
        }

        public void matchQueued(long requestId, Set<Integer> matchSizes) {
            send(Type.JOIN_MATCH, requestId, encodeUtf8("ENQUEUED:" + matchSizes));
        }

        public void matchLeaveAck(long requestId) {
            send(Type.LEAVE_MATCH, requestId, LEAVE_MATCH_BYTES);
        }

        public void placeStoneError(long requestId, String message) {
            send(Type.PLACE_STONE, requestId, ErrorMessageEncoder.encode(message));
        }

        public void leaveInGameAck(long requestId) {
            send(Type.LEAVE_IN_GAME_SESSION, requestId, LEAVE_GAME_BYTES);
        }

        public void notifyPeerLeft(String peerId) {
            send(Type.LEAVE_IN_GAME_SESSION, 0L, encodeUtf8("PEER_LEFT:" + Objects.requireNonNull(peerId, "peerId")));
        }

        private void send(Type type, long requestId, byte[] payload) {
            teamnova.omok.glue.client.session.log.ClientMessageLogger.outbound(handle, type, requestId);
            handle.enqueueResponse(type, requestId, payload == null ? new byte[0] : payload);
        }
    }

    private static byte[] encodeUtf8(String value) {
        return value == null ? new byte[0] : value.getBytes(StandardCharsets.UTF_8);
    }
}
