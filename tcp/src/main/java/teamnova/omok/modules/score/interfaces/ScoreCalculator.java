package teamnova.omok.modules.score.interfaces;

import teamnova.omok.modules.score.models.ScoreCalculationRequest;
import teamnova.omok.modules.score.models.ScoreCalculationResult;

/**
 * Defines the contract for computing score adjustments once a game resolves.
 */
public interface ScoreCalculator {
    ScoreCalculationResult calculate(ScoreCalculationRequest request);
}
