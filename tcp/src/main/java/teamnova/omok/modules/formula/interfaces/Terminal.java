package teamnova.omok.modules.formula.interfaces;

import java.util.function.Function;

import teamnova.omok.modules.formula.models.FormulaParams;
import teamnova.omok.modules.formula.terminals.result.FormulaResult;

/**
 * Terminal element of a formula pipeline.
 */
@FunctionalInterface
public interface Terminal extends Function<FormulaParams, FormulaResult> { }
