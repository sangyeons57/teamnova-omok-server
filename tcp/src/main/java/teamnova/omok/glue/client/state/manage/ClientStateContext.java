package teamnova.omok.glue.client.state.manage;

import java.util.Objects;
import java.util.Set;

import teamnova.omok.glue.client.session.ClientSessionManager;
import teamnova.omok.glue.client.session.interfaces.ClientSessionHandle;
import teamnova.omok.glue.client.session.services.ClientSessionStore;
import teamnova.omok.glue.game.session.GameSessionManager;
import teamnova.omok.glue.game.session.states.GameStateHub;
import teamnova.omok.glue.manager.MatchingManager;
import teamnova.omok.modules.state_machine.interfaces.StateContext;

/**
 * Shared data for client-level state handling.
 */
public final class ClientStateContext implements StateContext {
    private final ClientSessionHandle clientSession;
    private GameStateHub gameStateManager;
    private MatchingIntent pendingMatching;
    private boolean matchingQueued;

    public ClientStateContext(ClientSessionHandle clientSession) {
        this.clientSession = Objects.requireNonNull(clientSession, "clientSession");
    }

    public ClientSessionHandle clientSession() {
        return clientSession;
    }

    public GameStateHub gameStateManager() {
        return gameStateManager;
    }

    public void attachGame(GameStateHub manager) {
        this.gameStateManager = manager;
    }

    public void clearGame() {
        this.gameStateManager = null;
    }

    public void prepareMatching(Set<Integer> matchSizes, int rating, long requestId) {
        if (matchSizes == null || matchSizes.isEmpty()) {
            this.pendingMatching = null;
            return;
        }
        this.pendingMatching = new MatchingIntent(Set.copyOf(matchSizes), rating, requestId);
    }

    public void beginMatchingQueue() {
        MatchingIntent intent = this.pendingMatching;
        this.pendingMatching = null;
        if (intent == null || intent.matchSizes().isEmpty()) {
            return;
        }
        String userId = clientSession.authenticatedUserId();
        if (userId == null || userId.isBlank()) {
            return;
        }
        try {
            MatchingManager manager = MatchingManager.getInstance();
            manager.cancel(userId);
            manager.enqueue(userId, intent.rating(), intent.matchSizes());
            matchingQueued = true;
        } catch (Throwable ignore) {
            matchingQueued = false;
            return;
        }
        sendMatchJoinAck(intent.requestId(), intent.matchSizes());
    }

    public void cancelMatchingQueue(long ackRequestId) {
        boolean hadPending = pendingMatching != null;
        pendingMatching = null;
        boolean wasQueued = matchingQueued;
        if (matchingQueued) {
            String userId = clientSession.authenticatedUserId();
            if (userId != null) {
                try {
                    MatchingManager.getInstance().cancel(userId);
                } catch (Throwable ignore) {
                    // best-effort cancellation
                }
            }
            matchingQueued = false;
        }
        if (ackRequestId > 0 && (hadPending || wasQueued)) {
            sendMatchLeaveAck(ackRequestId);
        }
    }

    public void cleanupDisconnected() {

    }

    private void sendMatchJoinAck(long requestId, Set<Integer> matchSizes) {
        if (requestId <= 0) {
            return;
        }
        ClientSessionManager.getInstance()
            .clientPublisher(clientSession)
            .matchQueued(requestId, matchSizes);
    }

    private void sendMatchLeaveAck(long requestId) {
        if (requestId <= 0) {
            return;
        }
        ClientSessionManager.getInstance()
            .clientPublisher(clientSession)
            .matchLeaveAck(requestId);
    }

    private void notifyGameDisconnect(String userId) {
        try {
            GameSessionManager.getInstance().handleClientDisconnected(userId);
        } catch (Throwable ignore) {
            // swallow
        }
    }

    private record MatchingIntent(Set<Integer> matchSizes, int rating, long requestId) {
    }
}
