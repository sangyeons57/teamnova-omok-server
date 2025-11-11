package teamnova.omok.glue.handler;

import teamnova.omok.glue.client.session.interfaces.ClientSessionHandle;
import teamnova.omok.glue.client.session.log.ClientMessageLogger;
import teamnova.omok.glue.client.session.model.AuthResultStatus;
import teamnova.omok.glue.data.model.JWTPayload;
import teamnova.omok.glue.data.model.JwtVerificationException;
import teamnova.omok.glue.handler.register.FrameHandler;
import teamnova.omok.glue.manager.DataManager;
import teamnova.omok.glue.message.decoder.StringDecoder;
import teamnova.omok.core.nio.FramedMessage;
import teamnova.omok.core.nio.NioReactorServer;

public class AuthHandler implements FrameHandler {
    private final StringDecoder decoder;
    private final DataManager dataManager;

    public AuthHandler(StringDecoder decoder, DataManager dataManager){
        this.decoder = decoder;
        this.dataManager = dataManager;
    }

    @Override
    public void handle(NioReactorServer server, ClientSessionHandle session, FramedMessage frame) {
        ClientMessageLogger.inbound(session, teamnova.omok.glue.handler.register.Type.AUTH, frame.requestId());
        String jwt = decoder.decode(frame.payload());
        if (jwt == null || jwt.isBlank()) {
            session.clearAuthenticationBinding();
            System.err.println("JWT payload missing");
            session.sendAuthResult(frame.requestId(), AuthResultStatus.FAILURE);
            return;
        }

        try {
            JWTPayload payload = dataManager.verify(jwt.trim());
            session.authenticateUser(payload.userId(), payload.role(), payload.scope());
            AuthResultStatus status = AuthResultStatus.SUCCESS;
            if (session.currentGameSessionId() != null) {
                boolean rejoined = false;
                try {
                    rejoined = session.reconnectGameSession();
                } catch (RuntimeException ex) {
                    System.err.println("Reconnect flow failed: " + ex.getMessage());
                }
                status = rejoined ? AuthResultStatus.RECONNECTED : AuthResultStatus.FAILURE;
            }
            session.sendAuthResult(frame.requestId(), status);
        } catch (JwtVerificationException e) {
            session.clearAuthenticationBinding();
            System.err.println("JWT verification failed: " + e.getMessage());
            session.sendAuthResult(frame.requestId(), AuthResultStatus.FAILURE);
        }
    }
}
