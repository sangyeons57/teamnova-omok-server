package teamnova.omok.modules.score.services;

import java.util.Objects;

import teamnova.omok.modules.score.interfaces.ScoreCalculator;
import teamnova.omok.modules.score.models.ScoreCalculationRequest;
import teamnova.omok.modules.score.models.ScoreCalculationResult;
import teamnova.omok.modules.score.models.ScoreOutcome;

/**
 * Default implementation that applies fixed score deltas based on the outcome.
 */
public final class DefaultScoreCalculator implements ScoreCalculator {
    private final int winDelta;
    private final int lossDelta;
    private final int drawDelta;
    private final int disconnectedPenalty;

    public DefaultScoreCalculator() {
        this(10, -5, 0, -5);
    }

    public DefaultScoreCalculator(int winDelta, int lossDelta, int drawDelta, int disconnectedPenalty) {
        this.winDelta = winDelta;
        this.lossDelta = lossDelta;
        this.drawDelta = drawDelta;
        this.disconnectedPenalty = disconnectedPenalty;
    }

    @Override
    public ScoreCalculationResult calculate(ScoreCalculationRequest request) {
        Objects.requireNonNull(request, "request");
        int delta = baseDelta(request.outcome());
        if (request.disconnected()) {
            delta += disconnectedPenalty;
        }
        return new ScoreCalculationResult(delta);
    }

    private int baseDelta(ScoreOutcome outcome) {
        return switch (outcome) {
            case WIN -> winDelta;
            case LOSS -> lossDelta;
            case DRAW -> drawDelta;
            case PENDING -> 0;
        };
    }
}
