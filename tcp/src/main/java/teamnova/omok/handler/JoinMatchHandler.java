package teamnova.omok.handler;

import teamnova.omok.handler.register.FrameHandler;
import teamnova.omok.handler.register.Type;
import teamnova.omok.nio.ClientSession;
import teamnova.omok.nio.FramedMessage;
import teamnova.omok.nio.NioReactorServer;
import teamnova.omok.service.MatchingService;
import teamnova.omok.service.ServiceContainer;
import teamnova.omok.service.InGameSessionService;

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

        // Parse payload for rating and match size (very simple string format)
        int rating = 1000;
        int matchSize = 2;
        if (frame.payload() != null && frame.payload().length > 0) {
            String s = new String(frame.payload(), StandardCharsets.UTF_8);
            // accepted formats: "1234,2" or "rating=1234;match=2"
            try {
                if (s.contains(";")) {
                    String[] parts = s.split(";");
                    for (String p : parts) {
                        String[] kv = p.split("=");
                        if (kv.length == 2) {
                            if (kv[0].equalsIgnoreCase("rating")) rating = Integer.parseInt(kv[1]);
                            if (kv[0].equalsIgnoreCase("match")) matchSize = Integer.parseInt(kv[1]);
                        }
                    }
                } else if (s.contains(",")) {
                    String[] arr = s.split(",");
                    if (arr.length > 0) rating = Integer.parseInt(arr[0].trim());
                    if (arr.length > 1) matchSize = Integer.parseInt(arr[1].trim());
                } else if (!s.isBlank()) {
                    rating = Integer.parseInt(s.trim());
                }
            } catch (Exception ignore) { /* use defaults */ }
        }
        matchSize = Math.min(Math.max(matchSize, 2), 4); // clamp 2..4
        Set<Integer> matchSet = new HashSet<>();
        matchSet.add(matchSize);

        MatchingService matching = ServiceContainer.getInstance().getMatchingService();
        matching.enqueue(new MatchingService.Ticket(userId, rating, matchSet));

        // optional ack
        session.enqueueResponse(Type.JOIN_MATCH, frame.requestId(), ("ENQUEUED:" + matchSize).getBytes(StandardCharsets.UTF_8));
        server.enqueueSelectorTask(session::enableWriteInterest);
    }
}
