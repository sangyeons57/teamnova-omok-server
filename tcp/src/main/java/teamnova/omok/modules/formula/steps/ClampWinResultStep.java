package teamnova.omok.modules.formula.steps;

import teamnova.omok.modules.formula.interfaces.FormulaStep;
import teamnova.omok.modules.formula.models.FormulaParams;
import teamnova.omok.modules.formula.models.FormulaVariables;
import teamnova.omok.modules.formula.models.MatchOutcome;

/**
 * Ensures win outcomes remain within the allowed range.
 */
public final class ClampWinResultStep implements FormulaStep {
    private final double stageRequirementPoints;
    private final double stageRequirementWins;

    public ClampWinResultStep(double stageRequirementPoints, double stageRequirementWins) {
        this.stageRequirementPoints = stageRequirementPoints;
        this.stageRequirementWins = stageRequirementWins;
    }

    @Override
    public FormulaParams apply(FormulaParams params) {
        if (Boolean.TRUE.equals(params.get(FormulaVariables.TERMINATED))) {
            return params;
        }
        MatchOutcome outcome = params.get(FormulaVariables.OUTCOME);
        if (outcome != MatchOutcome.WIN) {
            return params;
        }
        double baseScore = params.getDouble(
            FormulaVariables.BASE_SCORE,
            stageRequirementWins <= 0.0 ? 0.0 : stageRequirementPoints / stageRequirementWins
        );
        double stageRequirement = params.getDouble(FormulaVariables.STAGE_REQUIREMENT_POINTS, stageRequirementPoints);
        double value = params.getDouble(FormulaVariables.RESULT, 0.0);
        if (value < baseScore) {
            return params.with(FormulaVariables.RESULT, baseScore);
        }
        if (value >= stageRequirement) {
            double upperBound = stageRequirement - 1.0;
            if (upperBound < baseScore) {
                upperBound = baseScore;
            }
            return params.with(FormulaVariables.RESULT, upperBound);
        }
        return params;
    }
}
