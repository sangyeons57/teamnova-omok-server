package teamnova.omok.modules.formula.steps;

import teamnova.omok.modules.formula.interfaces.FormulaStep;
import teamnova.omok.modules.formula.models.FormulaParams;
import teamnova.omok.modules.formula.models.FormulaVariables;
import teamnova.omok.modules.formula.models.MatchOutcome;

/**
 * Calculates the raw score delta based on the match outcome.
 */
public final class ResolveOutcomeStep implements FormulaStep {
    private final double stageRequirementPoints;
    private final double stageRequirementWins;
    private final double initialBonusStageLimit;
    private final double lossRatioDivisor;

    public ResolveOutcomeStep(double stageRequirementPoints,
                              double stageRequirementWins,
                              double initialBonusStageLimit,
                              double lossRatioDivisor) {
        this.stageRequirementPoints = stageRequirementPoints;
        this.stageRequirementWins = stageRequirementWins;
        this.initialBonusStageLimit = initialBonusStageLimit;
        this.lossRatioDivisor = lossRatioDivisor;
    }

    @Override
    public FormulaParams apply(FormulaParams params) {
        MatchOutcome outcome = params.get(FormulaVariables.OUTCOME);
        if (outcome == null || outcome == MatchOutcome.PENDING) {
            return params
                .with(FormulaVariables.RESULT, 0.0)
                .with(FormulaVariables.TERMINATED, Boolean.TRUE);
        }
        if (outcome == MatchOutcome.DRAW) {
            return params
                .with(FormulaVariables.RESULT, 0.0)
                .with(FormulaVariables.TERMINATED, Boolean.TRUE);
        }

        double baseScore = params.getDouble(
            FormulaVariables.BASE_SCORE,
            stageRequirementWins <= 0.0 ? 0.0 : stageRequirementPoints / stageRequirementWins
        );
        double playerScore = params.getDouble(FormulaVariables.PLAYER_SCORE, 0.0);
        double currentResult = params.getDouble(FormulaVariables.RESULT, 0.0);

        if (outcome == MatchOutcome.WIN) {
            double stageBonus = params.getDouble(FormulaVariables.STAGE_BONUS, baseScore / 2.0);
            double opponentScore = params.getDouble(FormulaVariables.OPPONENT_SCORE, playerScore);
            double comparative = 0.0;
            if (stageRequirementPoints > 0.0) {
                comparative = (opponentScore - playerScore) / stageRequirementPoints * stageBonus;
            }
            double threshold = params.getDouble(
                FormulaVariables.INITIAL_BONUS_THRESHOLD,
                stageRequirementPoints * initialBonusStageLimit
            );
            double streakBonus = params.getDouble(FormulaVariables.WIN_STREAK, 0.0);
            double updated = currentResult + baseScore + comparative + streakBonus;
            if (playerScore < threshold) {
                updated += baseScore;
            }
            return params
                .with(FormulaVariables.RESULT, updated)
                .with(FormulaVariables.TERMINATED, Boolean.FALSE);
        }

        if (outcome == MatchOutcome.LOSS) {
            double threshold = params.getDouble(FormulaVariables.INITIAL_BONUS_THRESHOLD, 0.0);
            double ratio = playerScore >= threshold && lossRatioDivisor > 0.0
                ? Math.floor(playerScore / lossRatioDivisor)
                : 0.0;
            double streakBonus = params.getDouble(FormulaVariables.WIN_STREAK, 0.0);
            double lossDelta = -(baseScore + ratio) + streakBonus;
            double updated = currentResult + lossDelta;
            return params
                .with(FormulaVariables.RESULT, updated)
                .with(FormulaVariables.TERMINATED, Boolean.FALSE);
        }

        return params.with(FormulaVariables.TERMINATED, Boolean.FALSE);
    }
}
