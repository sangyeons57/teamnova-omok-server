package teamnova.omok.modules.formula.steps;

import teamnova.omok.modules.formula.interfaces.FormulaStep;
import teamnova.omok.modules.formula.models.FormulaParams;
import teamnova.omok.modules.formula.models.FormulaVariables;

/**
 * Applies a penalty when the player disconnected mid-game.
 */
public final class DisconnectedPenaltyStep implements FormulaStep {
    private final double penalty;

    public DisconnectedPenaltyStep(double penalty) {
        this.penalty = penalty;
    }

    @Override
    public FormulaParams apply(FormulaParams params) {
        if (Boolean.TRUE.equals(params.get(FormulaVariables.TERMINATED))) {
            return params;
        }
        Boolean disconnected = params.get(FormulaVariables.DISCONNECTED);
        if (!Boolean.TRUE.equals(disconnected)) {
            return params;
        }
        double appliedPenalty = params.getDouble(FormulaVariables.DISCONNECTED_PENALTY, penalty);
        double result = params.getDouble(FormulaVariables.RESULT, 0.0) - Math.abs(appliedPenalty);
        return params.with(FormulaVariables.RESULT, result);
    }
}
