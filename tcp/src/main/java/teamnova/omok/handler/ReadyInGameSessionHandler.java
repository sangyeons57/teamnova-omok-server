package teamnova.omok.handler;

import teamnova.omok.handler.register.FrameHandler;
import teamnova.omok.handler.register.Type;
import teamnova.omok.message.encoder.ErrorMessageEncoder;
import teamnova.omok.message.encoder.ReadyStateMessageEncoder;
import teamnova.omok.nio.ClientSession;
import teamnova.omok.nio.FramedMessage;
import teamnova.omok.nio.NioReactorServer;
import teamnova.omok.service.InGameSessionService;
import teamnova.omok.service.ServiceContainer;

import java.util.Optional;

public class ReadyInGameSessionHandler implements FrameHandler {
    @Override
    public void handle(NioReactorServer server, ClientSession session, FramedMessage frame) {
        if (!session.isAuthenticated()) {
            return;
        }
        String userId = session.authenticatedUserId();
        InGameSessionService inGameService = ServiceContainer.getInstance().getInGameSessionService();

        Optional<InGameSessionService.ReadyResult> optional = inGameService.markReady(userId);
        byte[] payload;
        if (optional.isEmpty()) {
            payload = ErrorMessageEncoder.encode("SESSION_NOT_FOUND");
        } else {
            InGameSessionService.ReadyResult result = optional.get();
            if (!result.validUser()) {
                payload = ErrorMessageEncoder.encode("INVALID_PLAYER");
            } else {
                payload = ReadyStateMessageEncoder.encode(result);
            }
        }
        session.enqueueResponse(Type.READY_IN_GAME_SESSION, frame.requestId(), payload);
        server.enqueueSelectorTask(session::enableWriteInterest);
    }
}
