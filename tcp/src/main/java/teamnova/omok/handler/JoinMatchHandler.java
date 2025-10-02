package teamnova.omok.handler;

import teamnova.omok.handler.register.FrameHandler;
import teamnova.omok.handler.register.Type;
import teamnova.omok.nio.ClientSession;
import teamnova.omok.nio.FramedMessage;
import teamnova.omok.nio.NioReactorServer;
import teamnova.omok.service.MatchingService;
import teamnova.omok.service.ServiceContainer;
import teamnova.omok.service.InGameSessionService;
import teamnova.omok.service.MysqlService;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

public class JoinMatchHandler implements FrameHandler {
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
        String s = null;
        if (frame.payload() != null && frame.payload().length > 0) {
            s = new String(frame.payload(), StandardCharsets.UTF_8).trim();
        }
        if (s == null || s.isBlank()) {
            matchSet.add(2); // default 2 players
        } else if ("1".equals(s)) {
            // Free mode: allow 2,3,4
            matchSet.add(2);
            matchSet.add(3);
            matchSet.add(4);
        } else if ("2".equals(s) || "3".equals(s) || "4".equals(s)) {
            matchSet.add(Integer.parseInt(s));
        } else {
            // Fallback
            matchSet.add(2);
        }

        MatchingService matching = ServiceContainer.getInstance().getMatchingService();
        // Ensure previous ticket is removed to avoid duplicates
        matching.cancel(userId);
        matching.enqueue(new MatchingService.Ticket(userId, rating, matchSet));

        // optional ack
        session.enqueueResponse(Type.JOIN_MATCH, frame.requestId(), ("ENQUEUED:" + matchSet.toString()).getBytes(StandardCharsets.UTF_8));
        server.enqueueSelectorTask(session::enableWriteInterest);
    }
}
