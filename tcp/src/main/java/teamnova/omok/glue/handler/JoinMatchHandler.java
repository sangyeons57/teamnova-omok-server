package teamnova.omok.glue.handler;

import teamnova.omok.glue.client.session.ClientSessionManager;
import teamnova.omok.glue.handler.register.FrameHandler;
import teamnova.omok.glue.manager.DataManager;
import teamnova.omok.glue.manager.MatchingManager;
import teamnova.omok.glue.message.decoder.StringDecoder;
import teamnova.omok.glue.client.session.interfaces.ClientSessionHandle;
import teamnova.omok.core.nio.FramedMessage;
import teamnova.omok.core.nio.NioReactorServer;

import java.util.HashSet;
import java.util.Set;

public class JoinMatchHandler implements FrameHandler {
    private final StringDecoder stringDecoder;

    public JoinMatchHandler(StringDecoder stringDecoder) {
        this.stringDecoder = stringDecoder;
    }

    @Override
    public void handle(NioReactorServer server, ClientSessionHandle session, FramedMessage frame) {
        teamnova.omok.glue.client.session.log.ClientMessageLogger.inbound(session, teamnova.omok.glue.handler.register.Type.JOIN_MATCH, frame.requestId());
        if (!session.isAuthenticated()) {
            // ignore if not authenticated
            return;
        }

        String userId = session.authenticatedUserId();
        // Resolve rating from DB (users.score) using MysqlService, default to 1000
        int rating = DataManager.getInstance().getUserScore(userId, 1000).score();

        // Payload now only contains mode: one of "1","2","3","4"
        Set<Integer> matchSet = new HashSet<>();
        String payload = stringDecoder.decode(frame.payload());
        if (payload.isBlank()) {
            matchSet.add(2); // default 2 players
        } else if ("1".equals(payload)) {
            // Free mode: allow 2,3,4
            matchSet.add(2);
            matchSet.add(3);
            matchSet.add(4);
        } else if ("2".equals(payload) || "3".equals(payload) || "4".equals(payload)) {
            matchSet.add(Integer.parseInt(payload));
        } else {
            // Fallback
            matchSet.add(2);
        }
        // concise log for join match
        System.out.println("[MATCH][JOIN] user=" + userId + " mode=" + payload + " matchSet=" + matchSet + " rating=" + rating);

        // Ensure previous ticket is removed to avoid duplicates
        MatchingManager.getInstance().cancel(userId);
        MatchingManager.getInstance().enqueue(userId, rating, matchSet);

        // optional ack
        ClientSessionManager.getInstance()
            .clientPublisher(session)
            .matchQueued(frame.requestId(), matchSet);
    }
}
