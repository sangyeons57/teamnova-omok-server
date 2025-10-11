package teamnova.omok.service;

import java.util.Objects;
import java.util.Set;

import teamnova.omok.game.PlayerResult;
import teamnova.omok.store.GameSession;

/**
 * Applies post-game score adjustments to players using {@link MysqlService}.
 */
public final class ScoreService {
    private static final int WIN_DELTA = 10;
    private static final int LOSS_DELTA = -5;
    private static final int DRAW_DELTA = 0;
    private static final int DISCONNECTED_PENALTY = -5;

    private final MysqlService mysqlService;

    public ScoreService(MysqlService mysqlService) {
        this.mysqlService = Objects.requireNonNull(mysqlService, "mysqlService");
    }

    public void applyGameResults(GameSession session) {
        Objects.requireNonNull(session, "session");
        Set<String> disconnected = session.disconnectedUsersView();
        for (String userId : session.getUserIds()) {
            PlayerResult result = session.outcomeFor(userId);
            int delta = deltaFor(result);
            if (disconnected.contains(userId)) {
                delta += DISCONNECTED_PENALTY;
            }
            if (delta == 0) {
                continue;
            }
            boolean success = mysqlService.adjustUserScore(userId, delta);
            System.out.printf(
                "[ScoreService] session=%s user=%s result=%s disconnected=%s delta=%d applied=%s%n",
                session.getId(),
                userId,
                result,
                disconnected.contains(userId),
                delta,
                success
            );
        }
    }

    private int deltaFor(PlayerResult result) {
        if (result == null) {
            return 0;
        }
        return switch (result) {
            case WIN -> WIN_DELTA;
            case LOSS -> LOSS_DELTA;
            case DRAW -> DRAW_DELTA;
            case PENDING -> 0;
        };
    }
}
