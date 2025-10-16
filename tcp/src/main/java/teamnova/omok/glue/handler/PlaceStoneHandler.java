package teamnova.omok.glue.handler;

import teamnova.omok.glue.handler.register.FrameHandler;
import teamnova.omok.glue.handler.register.Type;
import teamnova.omok.glue.message.decoder.StringDecoder;
import teamnova.omok.glue.message.encoder.ErrorMessageEncoder;
import teamnova.omok.core.nio.ClientSession;
import teamnova.omok.core.nio.FramedMessage;
import teamnova.omok.core.nio.NioReactorServer;
import teamnova.omok.glue.service.InGameSessionService;
import teamnova.omok.glue.service.ServiceManager;

public class PlaceStoneHandler implements FrameHandler {
    private final StringDecoder stringDecoder;

    public PlaceStoneHandler(StringDecoder stringDecoder) {
        this.stringDecoder = stringDecoder;
    }

    @Override
    public void handle(NioReactorServer server, ClientSession session, FramedMessage frame) {
        if (!session.isAuthenticated()) {
            return;
        }
        String userId = session.authenticatedUserId();
        String payloadStr = stringDecoder.decode(frame.payload()).trim();

        int comma = payloadStr.indexOf(',');
        if (comma < 0) {
            respond(session, server, frame, ErrorMessageEncoder.encode("INVALID_PAYLOAD"));
            return;
        }
        int x, y;
        try {
            x = Integer.parseInt(payloadStr.substring(0, comma).trim());
            y = Integer.parseInt(payloadStr.substring(comma + 1).trim());
        } catch (NumberFormatException ex) {
            respond(session, server, frame, ErrorMessageEncoder.encode("INVALID_COORDINATES"));
            return;
        }

        InGameSessionService inGameService = ServiceManager.getInstance().getInGameSessionService();
        boolean accepted = inGameService.submitMove(userId, frame.requestId(), x, y);
        if (!accepted) {
            respond(session, server, frame, ErrorMessageEncoder.encode("SESSION_NOT_FOUND"));
        }
    }

    private void respond(ClientSession session, NioReactorServer server, FramedMessage frame, byte[] payload) {
        session.enqueueResponse(Type.PLACE_STONE, frame.requestId(), payload);
        server.enqueueSelectorTask(session::enableWriteInterest);
    }

}
