package teamnova.omok.modules.formula.steps;

import teamnova.omok.modules.formula.interfaces.FormulaStep;
import teamnova.omok.modules.formula.models.FormulaParams;
import teamnova.omok.modules.formula.models.FormulaVariables;

/**
 * Initializes derived variables required for formula calculations.
 */
public final class BootstrapStep implements FormulaStep {
    private static final double STAGE_BONUS_DIVISOR = 2.0;

    private final double stageRequirementPoints;
    private final double stageRequirementWins;
    private final double initialBonusStageLimit;
    private final double disconnectedPenalty;

    public BootstrapStep(double stageRequirementPoints,
                         double stageRequirementWins,
                         double initialBonusStageLimit,
                         double disconnectedPenalty) {
        this.stageRequirementPoints = stageRequirementPoints;
        this.stageRequirementWins = stageRequirementWins;
        this.initialBonusStageLimit = initialBonusStageLimit;
        this.disconnectedPenalty = disconnectedPenalty;
    }

    @Override
    public FormulaParams apply(FormulaParams params) {
        double baseScore = stageRequirementWins <= 0.0 ? 0.0 : stageRequirementPoints / stageRequirementWins;

        FormulaParams updated = params
            .with(FormulaVariables.BASE_SCORE, baseScore)
            .with(FormulaVariables.STAGE_BONUS, baseScore / STAGE_BONUS_DIVISOR)
            .with(FormulaVariables.STAGE_REQUIREMENT_POINTS, stageRequirementPoints)
            .with(FormulaVariables.STAGE_REQUIREMENT_WINS, stageRequirementWins)
            .with(FormulaVariables.INITIAL_BONUS_THRESHOLD, stageRequirementPoints * initialBonusStageLimit)
            .with(FormulaVariables.DISCONNECTED_PENALTY, disconnectedPenalty);

        if (!updated.contains(FormulaVariables.RESULT)) {
            updated = updated.with(FormulaVariables.RESULT, 0.0);
        }

        return updated.with(FormulaVariables.TERMINATED, Boolean.FALSE);
    }
}
