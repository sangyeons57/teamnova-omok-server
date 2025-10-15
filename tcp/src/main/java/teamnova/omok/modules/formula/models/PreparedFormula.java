package teamnova.omok.modules.formula.models;

import java.util.Objects;
import java.util.function.Function;

import teamnova.omok.modules.formula.interfaces.Terminal;
import teamnova.omok.modules.formula.terminals.result.FormulaResult;

/**
 * Represents a ready-to-execute formula pipeline.
 */
public final class PreparedFormula implements Terminal {
    private final Function<FormulaParams, FormulaParams> chain;
    private final Terminal terminal;

    public PreparedFormula(Function<FormulaParams, FormulaParams> chain,
                           Terminal terminal) {
        this.chain = Objects.requireNonNull(chain, "chain");
        this.terminal = Objects.requireNonNull(terminal, "terminal");
    }

    @Override
    public FormulaResult apply(FormulaParams params) {
        FormulaParams normalized = ensureResultDefault(Objects.requireNonNull(params, "params"));
        FormulaParams after = chain.apply(normalized);
        return terminal.apply(after);
    }

    public FormulaResult evaluate(FormulaRequest request) {
        Objects.requireNonNull(request, "request");
        return apply(FormulaParams.of(request.values()));
    }

    private FormulaParams ensureResultDefault(FormulaParams params) {
        if (params.contains(FormulaVariables.RESULT)) {
            return params;
        }
        return params.with(FormulaVariables.RESULT, 0.0);
    }
}
