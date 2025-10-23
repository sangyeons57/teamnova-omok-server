package teamnova.omok.glue.handler;

import teamnova.omok.glue.client.session.ClientSessionManager;
import teamnova.omok.glue.client.session.interfaces.ClientSessionHandle;
import teamnova.omok.glue.client.session.log.ClientMessageLogger;
import teamnova.omok.glue.handler.register.FrameHandler;
import teamnova.omok.glue.handler.register.Type;
import teamnova.omok.glue.message.decoder.StringDecoder;
import teamnova.omok.core.nio.FramedMessage;
import teamnova.omok.core.nio.NioReactorServer;
import teamnova.omok.glue.game.session.GameSessionManager;

public class PlaceStoneHandler implements FrameHandler {
    private final StringDecoder stringDecoder;

    public PlaceStoneHandler(StringDecoder stringDecoder) {
        this.stringDecoder = stringDecoder;
    }

    @Override
    public void handle(NioReactorServer server, ClientSessionHandle session, FramedMessage frame) {
        ClientMessageLogger.inbound(session, Type.PLACE_STONE, frame.requestId());
        if (!session.isAuthenticated()) {
            return;
        }
        String userId = session.authenticatedUserId();
        String payloadStr = stringDecoder.decode(frame.payload()).trim();

        int comma = payloadStr.indexOf(',');
        if (comma < 0) {
            respondError(session, frame, "INVALID_PAYLOAD");
            return;
        }
        int x, y;
        try {
            x = Integer.parseInt(payloadStr.substring(0, comma).trim());
            y = Integer.parseInt(payloadStr.substring(comma + 1).trim());
        } catch (NumberFormatException ex) {
            respondError(session, frame, "INVALID_COORDINATES");
            return;
        }

        GameSessionManager gameSessionManager = GameSessionManager.getInstance();
        boolean accepted = gameSessionManager.submitMove(userId, frame.requestId(), x, y);
        if (!accepted) {
            respondError(session, frame, "SESSION_NOT_FOUND");
        }
    }

    private void respondError(ClientSessionHandle session, FramedMessage frame, String message) {
        ClientSessionManager.getInstance()
            .clientPublisher(session)
            .placeStoneError(frame.requestId(), message);
    }

}
