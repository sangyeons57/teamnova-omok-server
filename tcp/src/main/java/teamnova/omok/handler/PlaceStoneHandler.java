package teamnova.omok.handler;

import teamnova.omok.handler.register.FrameHandler;
import teamnova.omok.handler.register.Type;
import teamnova.omok.message.decoder.StringDecoder;
import teamnova.omok.message.encoder.ErrorMessageEncoder;
import teamnova.omok.message.encoder.MoveAckMessageEncoder;
import teamnova.omok.nio.ClientSession;
import teamnova.omok.nio.FramedMessage;
import teamnova.omok.nio.NioReactorServer;
import teamnova.omok.service.InGameSessionService;
import teamnova.omok.service.ServiceContainer;

import java.util.Optional;

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

        InGameSessionService inGameService = ServiceContainer.getInstance().getInGameSessionService();
        Optional<InGameSessionService.MoveResult> optional = inGameService.placeStone(userId, x, y);
        if (optional.isEmpty()) {
            respond(session, server, frame, ErrorMessageEncoder.encode("SESSION_NOT_FOUND"));
            return;
        }
        byte[] payload = MoveAckMessageEncoder.encode(optional.get());
        respond(session, server, frame, payload);
    }

    private void respond(ClientSession session, NioReactorServer server, FramedMessage frame, byte[] payload) {
        session.enqueueResponse(Type.PLACE_STONE, frame.requestId(), payload);
        server.enqueueSelectorTask(session::enableWriteInterest);
    }

}
