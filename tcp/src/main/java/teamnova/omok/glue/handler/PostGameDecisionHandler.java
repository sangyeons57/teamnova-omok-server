package teamnova.omok.glue.handler;

import teamnova.omok.core.nio.FramedMessage;
import teamnova.omok.core.nio.NioReactorServer;
import teamnova.omok.glue.client.session.interfaces.ClientSessionHandle;
import teamnova.omok.glue.game.session.model.PostGameDecision;
import teamnova.omok.glue.handler.register.FrameHandler;
import teamnova.omok.glue.message.decoder.StringDecoder;

public class PostGameDecisionHandler implements FrameHandler {
    private final StringDecoder stringDecoder;

    public PostGameDecisionHandler(StringDecoder stringDecoder) {
        this.stringDecoder = stringDecoder;
    }

    @Override
    public void handle(NioReactorServer server, ClientSessionHandle session, FramedMessage frame) {
        teamnova.omok.glue.client.session.log.ClientMessageLogger.inbound(session, teamnova.omok.glue.handler.register.Type.POST_GAME_DECISION, frame.requestId());
        if (!session.isAuthenticated()) {
            return;
        }
        String raw = stringDecoder.decode(frame.payload());
        String trimmed = raw == null ? "" : raw.trim();
        if (trimmed.isEmpty()) {
            return;
        }
        PostGameDecision decision;
        try {
            decision = PostGameDecision.valueOf(trimmed.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return;
        }

        session.submitPostGameDecision(frame.requestId(), decision);
    }
}
