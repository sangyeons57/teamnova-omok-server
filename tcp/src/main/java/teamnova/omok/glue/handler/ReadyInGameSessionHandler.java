package teamnova.omok.glue.handler;

import teamnova.omok.glue.handler.register.FrameHandler;
import teamnova.omok.glue.handler.register.Type;
import teamnova.omok.glue.message.encoder.ErrorMessageEncoder;
import teamnova.omok.core.nio.ClientSession;
import teamnova.omok.core.nio.FramedMessage;
import teamnova.omok.core.nio.NioReactorServer;
import teamnova.omok.glue.service.InGameSessionService;
import teamnova.omok.glue.service.ServiceManager;

public class ReadyInGameSessionHandler implements FrameHandler {
    @Override
    public void handle(NioReactorServer server, ClientSession session, FramedMessage frame) {
        if (!session.isAuthenticated()) {
            return;
        }
        String userId = session.authenticatedUserId();
        InGameSessionService inGameService = ServiceManager.getInstance().getInGameSessionService();

        boolean accepted = inGameService.submitReady(userId, frame.requestId());
        if (!accepted) {
            byte[] payload = ErrorMessageEncoder.encode("SESSION_NOT_FOUND");
            session.enqueueResponse(Type.READY_IN_GAME_SESSION, frame.requestId(), payload);
            server.enqueueSelectorTask(session::enableWriteInterest);
        }
    }
}
