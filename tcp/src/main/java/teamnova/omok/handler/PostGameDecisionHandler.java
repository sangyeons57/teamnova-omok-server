package teamnova.omok.handler;

import teamnova.omok.game.PostGameDecision;
import teamnova.omok.handler.register.FrameHandler;
import teamnova.omok.handler.register.Type;
import teamnova.omok.message.decoder.StringDecoder;
import teamnova.omok.message.encoder.PostGameDecisionAckMessageEncoder;
import teamnova.omok.nio.ClientSession;
import teamnova.omok.nio.FramedMessage;
import teamnova.omok.nio.NioReactorServer;
import teamnova.omok.service.InGameSessionService;
import teamnova.omok.service.InGameSessionService.PostGameDecisionResult;
import teamnova.omok.service.InGameSessionService.PostGameDecisionStatus;
import teamnova.omok.service.ServiceContainer;

public class PostGameDecisionHandler implements FrameHandler {
    private final StringDecoder stringDecoder;

    public PostGameDecisionHandler(StringDecoder stringDecoder) {
        this.stringDecoder = stringDecoder;
    }

    @Override
    public void handle(NioReactorServer server, ClientSession session, FramedMessage frame) {
        if (!session.isAuthenticated()) {
            return;
        }
        String userId = session.authenticatedUserId();
        String raw = stringDecoder.decode(frame.payload());
        String trimmed = raw == null ? "" : raw.trim();
        if (trimmed.isEmpty()) {
            respondImmediate(server, session, frame.requestId(), userId, PostGameDecisionStatus.INVALID_PAYLOAD);
            return;
        }
        PostGameDecision decision;
        try {
            decision = PostGameDecision.valueOf(trimmed.toUpperCase());
        } catch (IllegalArgumentException ex) {
            respondImmediate(server, session, frame.requestId(), userId, PostGameDecisionStatus.INVALID_PAYLOAD);
            return;
        }

        InGameSessionService inGameService = ServiceContainer.getInstance().getInGameSessionService();
        boolean accepted = inGameService.submitPostGameDecision(userId, frame.requestId(), decision);
        if (!accepted) {
            respondImmediate(server, session, frame.requestId(), userId, PostGameDecisionStatus.SESSION_NOT_FOUND);
        }
    }

    private void respondImmediate(NioReactorServer server,
                                  ClientSession session,
                                  long requestId,
                                  String userId,
                                  PostGameDecisionStatus status) {
        PostGameDecisionResult result = PostGameDecisionResult.rejected(null, userId, status);
        byte[] payload = PostGameDecisionAckMessageEncoder.encode(result);
        session.enqueueResponse(Type.POST_GAME_DECISION, requestId, payload);
        server.enqueueSelectorTask(session::enableWriteInterest);
    }
}
