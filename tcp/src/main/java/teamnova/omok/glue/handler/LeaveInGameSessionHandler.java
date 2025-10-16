package teamnova.omok.glue.handler;

import teamnova.omok.glue.handler.register.FrameHandler;
import teamnova.omok.glue.handler.register.Type;
import teamnova.omok.core.nio.ClientSession;
import teamnova.omok.core.nio.FramedMessage;
import teamnova.omok.core.nio.NioReactorServer;
import teamnova.omok.glue.service.InGameSessionService;
import teamnova.omok.glue.service.ServiceManager;

import java.nio.charset.StandardCharsets;

public class LeaveInGameSessionHandler implements FrameHandler {
    @Override
    public void handle(NioReactorServer server, ClientSession session, FramedMessage frame) {
        if (!session.isAuthenticated()) {
            return;
        }
        String userId = session.authenticatedUserId();
        InGameSessionService igs = ServiceManager.getInstance().getInGameSessionService();

        // Notify other users in the same session, if any
        igs.findByUser(userId).ifPresent(gs -> {
            for (String uid : gs.getUserIds()) {
                if (!uid.equals(userId)) {
                    ClientSession peer = igs.getClient(uid);
                    if (peer != null) {
                        peer.enqueueResponse(Type.LEAVE_IN_GAME_SESSION, 0L, ("PEER_LEFT:" + userId).getBytes(StandardCharsets.UTF_8));
                        server.enqueueSelectorTask(peer::enableWriteInterest);
                    }
                }
            }
        });

        igs.leaveByUser(userId);
        igs.unregisterClient(userId);
        session.enqueueResponse(Type.LEAVE_IN_GAME_SESSION, frame.requestId(), "LEFT".getBytes(StandardCharsets.UTF_8));
        server.enqueueSelectorTask(session::enableWriteInterest);
    }
}
