package teamnova.omok.modules.score;

import java.util.Objects;

import teamnova.omok.modules.score.interfaces.ScoreCalculator;
import teamnova.omok.modules.score.models.ScoreCalculationRequest;
import teamnova.omok.modules.score.models.ScoreCalculationResult;
import teamnova.omok.modules.score.services.DefaultScoreCalculator;

/**
 * Entry point for interacting with the score calculation module.
 */
public final class ScoreGateway {
    private ScoreGateway() { }

    public static Handle open() {
        return new Handle(new DefaultScoreCalculator());
    }

    public static Handle wrap(ScoreCalculator calculator) {
        return new Handle(calculator);
    }

    public static final class Handle {
        private final ScoreCalculator delegate;

        private Handle(ScoreCalculator delegate) {
            this.delegate = Objects.requireNonNull(delegate, "delegate");
        }

        public ScoreCalculationResult calculate(ScoreCalculationRequest request) {
            return delegate.calculate(request);
        }
    }
}
