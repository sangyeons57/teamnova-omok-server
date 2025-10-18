package teamnova.omok.glue.handler;

import teamnova.omok.glue.client.session.ClientSessionManager;
import teamnova.omok.glue.client.session.interfaces.ClientSessionHandle;
import teamnova.omok.glue.handler.register.FrameHandler;
import teamnova.omok.core.nio.FramedMessage;
import teamnova.omok.core.nio.NioReactorServer;
import teamnova.omok.glue.game.session.GameSessionManager;
import teamnova.omok.glue.service.ServiceManager;

public class LeaveInGameSessionHandler implements FrameHandler {
    @Override
    public void handle(NioReactorServer server, ClientSessionHandle session, FramedMessage frame) {
        if (!session.isAuthenticated()) {
            return;
        }
        String userId = session.authenticatedUserId();
        GameSessionManager gameSessionManager = ServiceManager.getInstance().getGameSessionManager();
        ClientSessionManager manager = ClientSessionManager.getInstance();

        // Notify other users in the same session, if any
        gameSessionManager.findSession(userId).ifPresent(gs -> {
            for (String uid : gs.getUserIds()) {
                if (!uid.equals(userId)) {
                    manager.clientPublisher(uid).ifPresent(channel -> channel.notifyPeerLeft(userId));
                }
            }
        });

        gameSessionManager.leaveSession(userId);
        manager.clientPublisher(session).leaveInGameAck(frame.requestId());
    }
}
