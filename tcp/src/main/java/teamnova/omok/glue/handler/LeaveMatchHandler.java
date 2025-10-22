package teamnova.omok.glue.handler;

import teamnova.omok.glue.client.session.ClientSessionManager;
import teamnova.omok.glue.client.session.interfaces.ClientSessionHandle;
import teamnova.omok.core.nio.FramedMessage;
import teamnova.omok.core.nio.NioReactorServer;
import teamnova.omok.glue.handler.register.FrameHandler;
import teamnova.omok.glue.manager.MatchingManager;

public class LeaveMatchHandler implements FrameHandler {
    @Override
    public void handle(NioReactorServer server, ClientSessionHandle session, FramedMessage frame) {
        teamnova.omok.glue.client.session.log.ClientMessageLogger.inbound(session, teamnova.omok.glue.handler.register.Type.LEAVE_MATCH, frame.requestId());
        if (session == null || server == null || !session.isAuthenticated()) {
            return;
        }

        String userId = session.authenticatedUserId();
        if (userId == null || userId.isBlank()) {
            return;
        }

        MatchingManager.getInstance().cancel(userId);
        System.out.println("[MATCH][LEAVE] user=" + userId);

        ClientSessionManager.getInstance()
            .clientPublisher(session)
            .matchLeaveAck(frame.requestId());
    }
}
