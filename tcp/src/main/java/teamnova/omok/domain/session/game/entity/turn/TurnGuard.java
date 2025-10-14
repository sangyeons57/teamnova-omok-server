package teamnova.omok.domain.session.game.entity.turn;

import java.util.List;

public class TurnGuard {
    public static void durationsGuard(long durationMillis) {
        if (durationMillis <= 0) {
            throw new IllegalArgumentException("durationMillis must be positive");
        }
    }

    public static void requirePlayersGuard(List<String> userOrder) {
        if (userOrder == null || userOrder.isEmpty()) {
            throw new IllegalStateException("No players registered for turn management");
        }
    }

}
