package teamnova.omok.glue.handler;

import teamnova.omok.glue.game.session.model.PostGameDecision;
import teamnova.omok.glue.client.session.ClientSessionManager;
import teamnova.omok.glue.client.session.interfaces.ClientSessionHandle;
import teamnova.omok.glue.game.session.GameSessionManager;
import teamnova.omok.glue.handler.register.FrameHandler;
import teamnova.omok.glue.message.decoder.StringDecoder;
import teamnova.omok.core.nio.FramedMessage;
import teamnova.omok.core.nio.NioReactorServer;
import teamnova.omok.glue.game.session.model.result.PostGameDecisionResult;
import teamnova.omok.glue.game.session.model.result.PostGameDecisionStatus;

public class PostGameDecisionHandler implements FrameHandler {
    private final StringDecoder stringDecoder;

    public PostGameDecisionHandler(StringDecoder stringDecoder) {
        this.stringDecoder = stringDecoder;
    }

    @Override
    public void handle(NioReactorServer server, ClientSessionHandle session, FramedMessage frame) {
        if (!session.isAuthenticated()) {
            return;
        }
        String userId = session.authenticatedUserId();
        String raw = stringDecoder.decode(frame.payload());
        String trimmed = raw == null ? "" : raw.trim();
        if (trimmed.isEmpty()) {
            respondImmediate(frame.requestId(), userId, PostGameDecisionStatus.INVALID_PAYLOAD);
            return;
        }
        PostGameDecision decision;
        try {
            decision = PostGameDecision.valueOf(trimmed.toUpperCase());
        } catch (IllegalArgumentException ex) {
            respondImmediate(frame.requestId(), userId, PostGameDecisionStatus.INVALID_PAYLOAD);
            return;
        }

        GameSessionManager gameSessionManager = GameSessionManager.getInstance();
        boolean accepted = gameSessionManager.submitPostGameDecision(userId, frame.requestId(), decision);
        if (!accepted) {
            respondImmediate(frame.requestId(), userId, PostGameDecisionStatus.SESSION_NOT_FOUND);
        }
    }

    private void respondImmediate(long requestId,
                                  String userId,
                                  PostGameDecisionStatus status) {
        PostGameDecisionResult result = PostGameDecisionResult.rejected(userId, status);
        ClientSessionManager.getInstance()
            .gamePublisher()
            .respondPostGameDecision(userId, requestId, result);
    }
}
