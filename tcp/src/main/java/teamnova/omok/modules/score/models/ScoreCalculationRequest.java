package teamnova.omok.modules.score.models;

import java.util.Objects;

/**
 * Captures the data required to compute a score adjustment for a player.
 */
public record ScoreCalculationRequest(ScoreOutcome outcome, boolean disconnected) {
    public ScoreCalculationRequest {
        Objects.requireNonNull(outcome, "outcome");
    }
}
