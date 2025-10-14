package teamnova.omok.glue.handler;

import teamnova.omok.glue.game.PostGameDecision;
import teamnova.omok.glue.handler.register.FrameHandler;
import teamnova.omok.glue.handler.register.Type;
import teamnova.omok.glue.message.decoder.StringDecoder;
import teamnova.omok.glue.message.encoder.PostGameDecisionAckMessageEncoder;
import teamnova.omok.core.nio.ClientSession;
import teamnova.omok.core.nio.FramedMessage;
import teamnova.omok.core.nio.NioReactorServer;
import teamnova.omok.glue.service.InGameSessionService;
import teamnova.omok.glue.service.dto.PostGameDecisionResult;
import teamnova.omok.glue.service.dto.PostGameDecisionStatus;
import teamnova.omok.glue.service.ServiceContainer;

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
