package teamnova.omok.modules.formula;

import java.util.Objects;

import teamnova.omok.modules.formula.interfaces.PipelineBuilder;
import teamnova.omok.modules.formula.interfaces.Terminal;
import teamnova.omok.modules.formula.models.FormulaParams;
import teamnova.omok.modules.formula.models.FormulaRequest;
import teamnova.omok.modules.formula.terminals.result.FormulaResult;
import teamnova.omok.modules.formula.models.PreparedFormula;
import teamnova.omok.modules.formula.services.DefaultPipelineBuilder;

/**
 * Thin gateway that exposes the formula pipeline facilities without encoding domain rules.
 */
public final class FormulaGateway {
    private FormulaGateway() { }

    public static PipelineBuilder pipeline() {
        return DefaultPipelineBuilder.builder();
    }

    public static Handle wrap(Terminal terminal) {
        return new Handle(Objects.requireNonNull(terminal, "terminal"));
    }

    public static Handle wrapPrepared(PreparedFormula formula) {
        return new Handle(Objects.requireNonNull(formula, "formula"));
    }

    public static final class Handle {
        private final Terminal delegate;

        Handle(Terminal delegate) {
            this.delegate = Objects.requireNonNull(delegate, "delegate");
        }

        public FormulaResult evaluate(FormulaRequest request) {
            Objects.requireNonNull(request, "request");
            return delegate.apply(FormulaParams.of(request.values()));
        }

        public FormulaResult apply(FormulaParams params) {
            return delegate.apply(Objects.requireNonNull(params, "params"));
        }
    }
}
