package teamnova.omok.modules.formula.terminals;

import teamnova.omok.modules.formula.interfaces.Terminal;
import teamnova.omok.modules.formula.models.FormulaParams;
import teamnova.omok.modules.formula.models.FormulaVariables;
import teamnova.omok.modules.formula.terminals.result.FormulaResult;

/**
 * Converts the accumulated result into the final integer delta.
 */
public final class FinalizeTerminal implements Terminal {
    @Override
    public FormulaResult apply(FormulaParams params) {
        double value = params.getDouble(FormulaVariables.RESULT, 0.0);
        return new FormulaResult(safeRound(value));
    }

    private int safeRound(double value) {
        long rounded = Math.round(value);
        if (rounded > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (rounded < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return (int) rounded;
    }
}
