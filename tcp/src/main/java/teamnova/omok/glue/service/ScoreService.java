package teamnova.omok.glue.service;

import java.util.Objects;
import java.util.Set;

import teamnova.omok.glue.game.PlayerResult;
import teamnova.omok.glue.store.GameSession;
import teamnova.omok.modules.score.ScoreGateway;
import teamnova.omok.modules.score.models.ScoreCalculationRequest;
import teamnova.omok.modules.score.models.ScoreOutcome;

/**
 * Applies post-game score adjustments to players using {@link MysqlService}.
 */
public final class ScoreService {
    private final MysqlService mysqlService;
    private final ScoreGateway.Handle scoreGateway;

    public ScoreService(MysqlService mysqlService) {
        this(mysqlService, ScoreGateway.open());
    }

    ScoreService(MysqlService mysqlService, ScoreGateway.Handle scoreGateway) {
        this.mysqlService = Objects.requireNonNull(mysqlService, "mysqlService");
        this.scoreGateway = Objects.requireNonNull(scoreGateway, "scoreGateway");
    }

    public void applyGameResults(GameSession session) {
        Objects.requireNonNull(session, "session");
        Set<String> disconnected = session.disconnectedUsersView();
        for (String userId : session.getUserIds()) {
            PlayerResult result = session.outcomeFor(userId);
            boolean isDisconnected = disconnected.contains(userId);
            ScoreOutcome outcome = mapOutcome(result);
            int delta = scoreGateway.calculate(new ScoreCalculationRequest(outcome, isDisconnected)).delta();
            if (delta == 0) {
                continue;
            }
            boolean success = mysqlService.adjustUserScore(userId, delta);
            System.out.printf(
                "[ScoreService] session=%s user=%s result=%s disconnected=%s delta=%d applied=%s%n",
                session.getId(),
                userId,
                result,
                isDisconnected,
                delta,
                success
            );
        }
    }

    private ScoreOutcome mapOutcome(PlayerResult result) {
        if (result == null) {
            return ScoreOutcome.PENDING;
        }
        return switch (result) {
            case WIN -> ScoreOutcome.WIN;
            case LOSS -> ScoreOutcome.LOSS;
            case DRAW -> ScoreOutcome.DRAW;
            case PENDING -> ScoreOutcome.PENDING;
        };
    }
}
