package teamnova.omok.glue.handler;

import teamnova.omok.glue.message.decoder.StringDecoder;
import teamnova.omok.glue.handler.register.FrameHandler;
import teamnova.omok.glue.handler.register.Type;
import teamnova.omok.core.nio.ClientSession;
import teamnova.omok.core.nio.FramedMessage;
import teamnova.omok.core.nio.NioReactorServer;
import teamnova.omok.glue.service.MatchingService;
import teamnova.omok.glue.service.ServiceContainer;
import teamnova.omok.glue.service.InGameSessionService;
import teamnova.omok.glue.service.MysqlService;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

public class JoinMatchHandler implements FrameHandler {
    private final StringDecoder stringDecoder;

    public JoinMatchHandler(StringDecoder stringDecoder) {
        this.stringDecoder = stringDecoder;
    }

    @Override
    public void handle(NioReactorServer server, ClientSession session, FramedMessage frame) {
        if (!session.isAuthenticated()) {
            // ignore if not authenticated
            return;
        }

        String userId = session.authenticatedUserId();
        // Register client for later broadcasting
        InGameSessionService igs = ServiceContainer.getInstance().getInGameSessionService();
        igs.registerClient(userId, session);

        // Resolve rating from DB (users.score) using MysqlService, default to 1000
        MysqlService mysql = ServiceContainer.getInstance().getMysqlService();
        int rating = (mysql != null) ? mysql.getUserScore(userId, 1000) : 1000;

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

        MatchingService matching = ServiceContainer.getInstance().getMatchingService();
        // Ensure previous ticket is removed to avoid duplicates
        matching.cancel(userId);
        matching.enqueue(new MatchingService.Ticket(userId, rating, matchSet));

        // optional ack
        session.enqueueResponse(Type.JOIN_MATCH, frame.requestId(), ("ENQUEUED:" + matchSet).getBytes(StandardCharsets.UTF_8));
        server.enqueueSelectorTask(session::enableWriteInterest);
    }
}
