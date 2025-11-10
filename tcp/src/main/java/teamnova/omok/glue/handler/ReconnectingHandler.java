package teamnova.omok.glue.handler;

import java.util.UUID;

import teamnova.omok.core.nio.FramedMessage;
import teamnova.omok.core.nio.NioReactorServer;
import teamnova.omok.glue.client.session.interfaces.ClientSessionHandle;
import teamnova.omok.glue.client.session.log.ClientMessageLogger;
import teamnova.omok.glue.data.model.JWTPayload;
import teamnova.omok.glue.data.model.JwtVerificationException;
import teamnova.omok.glue.game.session.model.vo.GameSessionId;
import teamnova.omok.glue.handler.register.FrameHandler;
import teamnova.omok.glue.handler.register.Type;
import teamnova.omok.glue.manager.DataManager;
import teamnova.omok.glue.message.decoder.StringDecoder;

public class ReconnectingHandler implements FrameHandler {
    private final StringDecoder decoder;
    private final DataManager dataManager;

    public ReconnectingHandler(StringDecoder decoder, DataManager dataManager){
        this.decoder = decoder;
        this.dataManager = dataManager;
    }

    @Override
    public void handle(NioReactorServer server, ClientSessionHandle session, FramedMessage frame) {
        ClientMessageLogger.inbound(session, Type.RECONNECTING, frame.requestId());
        String decoded = decoder.decode(frame.payload());
        if (decoded == null) {
            session.sendReconnectResult(frame.requestId(), false, "INVALID_PAYLOAD");
            return;
        }
        String[] parts = decoded.split(":", 2);
        String jwt = parts.length > 0 ? parts[0].trim() : "";
        String gameSessionToken = parts.length > 1 ? parts[1].trim() : "";

        if (jwt.isBlank()) {
            session.clearAuthenticationBinding();
            session.sendReconnectResult(frame.requestId(), false, "MISSING_JWT");
            return;
        }

        boolean rejoinRequested = !gameSessionToken.isBlank();
        GameSessionId expectedSessionId = null;
        if (rejoinRequested) {
            try {
                expectedSessionId = new GameSessionId(UUID.fromString(gameSessionToken));
            } catch (IllegalArgumentException ex) {
                expectedSessionId = null; // treat as auth-only success
            }
        }

        try {
            JWTPayload payload = dataManager.verify(jwt);
            session.authenticateUser(payload.userId(), payload.role(), payload.scope());
            boolean rejoined = false;
            if (rejoinRequested) {
                session.beginReconnectFlow();
                rejoined = expectedSessionId != null && session.reconnectGameSession(expectedSessionId);
                session.finishReconnectFlow(rejoined);
            }
            String detail = rejoined ? "REJOINED" : "AUTH_ONLY";
            session.sendReconnectResult(frame.requestId(), true, detail);
        } catch (JwtVerificationException e) {
            session.clearAuthenticationBinding();
            if (rejoinRequested) {
                session.finishReconnectFlow(false);
            }
            session.sendReconnectResult(frame.requestId(), false, "INVALID_TOKEN");
        }
    }
}
