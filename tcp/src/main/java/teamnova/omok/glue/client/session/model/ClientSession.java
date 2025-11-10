package teamnova.omok.glue.client.session.model;

import java.util.Objects;

import teamnova.omok.glue.game.session.model.PlayerResult;
import teamnova.omok.glue.game.session.model.vo.GameSessionId;

/**
 * Holds authentication attributes and per-client match metrics.
 */
public final class ClientSession {
    private volatile boolean authenticated;
    private volatile String userId;
    private volatile String role;
    private volatile String scope;

    // Currently bound in-game session for scoping outbound traffic
    private volatile GameSessionId currentGameSessionId;

    private final Object metricsLock = new Object();
    private int totalWins;
    private int totalLosses;
    private int totalDraws;
    private int consecutiveWins;

    public void markAuthenticated(String userId, String role, String scope) {
        this.userId = Objects.requireNonNull(userId, "userId");
        this.role = role;
        this.scope = scope;
        this.authenticated = true;
    }

    public void clearAuthentication() {
        this.authenticated = false;
        this.userId = null;
        this.role = null;
        this.scope = null;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public String userId() {
        return userId;
    }

    public String role() {
        return role;
    }

    public String scope() {
        return scope;
    }

    public GameSessionId currentGameSessionId() {
        return currentGameSessionId;
    }

    public void bindGameSession(GameSessionId id) {
        this.currentGameSessionId = id;
    }

    public void unbindGameSession(GameSessionId id) {
        if (Objects.equals(this.currentGameSessionId, id)) {
            this.currentGameSessionId = null;
        }
    }

    public void clearGameSession() {
        this.currentGameSessionId = null;
    }

    /**
     * Updates internal win/loss/draw counters based on the supplied game result.
     *
     * @return current win streak after the update
     */
    public int registerOutcome(PlayerResult result) {
        if (result == null) {
            return currentWinStreak();
        }
        synchronized (metricsLock) {
            return switch (result) {
                case WIN -> {
                    consecutiveWins = Math.max(0, consecutiveWins) + 1;
                    totalWins += 1;
                    yield consecutiveWins;
                }
                case LOSS -> {
                    int streak = consecutiveWins;
                    consecutiveWins = 0;
                    totalLosses += 1;
                    yield streak;
                }
                case DRAW -> {
                    totalDraws += 1;
                    yield consecutiveWins;
                }
                case PENDING -> consecutiveWins;
            };
        }
    }

    public int currentWinStreak() {
        synchronized (metricsLock) {
            return consecutiveWins;
        }
    }

    public int totalWins() {
        synchronized (metricsLock) {
            return totalWins;
        }
    }

    public int totalLosses() {
        synchronized (metricsLock) {
            return totalLosses;
        }
    }

    public int totalDraws() {
        synchronized (metricsLock) {
            return totalDraws;
        }
    }

    public ClientSessionMetrics metricsSnapshot() {
        synchronized (metricsLock) {
            return new ClientSessionMetrics(consecutiveWins, totalWins, totalLosses, totalDraws);
        }
    }

    public record ClientSessionMetrics(int winStreak, int totalWins, int totalLosses, int totalDraws) { }
}
