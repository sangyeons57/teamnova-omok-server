package teamnova.omok.modules.formula.interfaces;

import java.util.function.Function;

import teamnova.omok.modules.formula.models.FormulaParams;

/**
 * Represents a single immutable transformation in a formula pipeline.
 */
@FunctionalInterface
public interface FormulaStep extends Function<FormulaParams, FormulaParams> { }
