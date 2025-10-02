package teamnova.omok.handler;

import teamnova.omok.handler.register.FrameHandler;
import teamnova.omok.handler.register.Type;
import teamnova.omok.nio.ClientSession;
import teamnova.omok.nio.FramedMessage;
import teamnova.omok.nio.NioReactorServer;
import teamnova.omok.service.InGameSessionService;
import teamnova.omok.service.ServiceContainer;

import java.nio.charset.StandardCharsets;

public class LeaveInGameSessionHandler implements FrameHandler {
    @Override
    public void handle(NioReactorServer server, ClientSession session, FramedMessage frame) {
        if (!session.isAuthenticated()) {
            return;
        }
        String userId = session.authenticatedUserId();
        InGameSessionService igs = ServiceContainer.getInstance().getInGameSessionService();
        igs.leaveByUser(userId);
        igs.unregisterClient(userId);
        session.enqueueResponse(Type.LEAVE_IN_GAME_SESSION, frame.requestId(), "LEFT".getBytes(StandardCharsets.UTF_8));
        server.enqueueSelectorTask(session::enableWriteInterest);
    }
}
