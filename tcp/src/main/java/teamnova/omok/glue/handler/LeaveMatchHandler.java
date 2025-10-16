package teamnova.omok.glue.handler;

import java.nio.charset.StandardCharsets;

import teamnova.omok.core.nio.ClientSession;
import teamnova.omok.core.nio.FramedMessage;
import teamnova.omok.core.nio.NioReactorServer;
import teamnova.omok.glue.handler.register.FrameHandler;
import teamnova.omok.glue.handler.register.Type;
import teamnova.omok.glue.manager.MatchingManager;
import teamnova.omok.modules.matching.MatchingGateway;
import teamnova.omok.glue.service.ServiceManager;

public class LeaveMatchHandler implements FrameHandler {
    @Override
    public void handle(NioReactorServer server, ClientSession session, FramedMessage frame) {
        if (session == null || server == null || !session.isAuthenticated()) {
            return;
        }

        String userId = session.authenticatedUserId();
        if (userId == null || userId.isBlank()) {
            return;
        }

        MatchingManager.getInstance().cancel(userId);
        System.out.println("[MATCH][LEAVE] user=" + userId);

        session.enqueueResponse(Type.LEAVE_MATCH, frame.requestId(), "CANCELLED".getBytes(StandardCharsets.UTF_8));
        server.enqueueSelectorTask(session::enableWriteInterest);
    }
}
