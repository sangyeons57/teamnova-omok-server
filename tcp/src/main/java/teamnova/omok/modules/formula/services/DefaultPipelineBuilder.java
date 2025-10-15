package teamnova.omok.modules.formula.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import teamnova.omok.modules.formula.interfaces.FormulaStep;
import teamnova.omok.modules.formula.interfaces.PipelineBuilder;
import teamnova.omok.modules.formula.interfaces.Terminal;
import teamnova.omok.modules.formula.models.FormulaParams;
import teamnova.omok.modules.formula.models.PreparedFormula;
import teamnova.omok.modules.formula.steps.BootstrapStep;

/**
 * Utility for composing formula pipelines from discrete steps.
 */
public final class DefaultPipelineBuilder implements PipelineBuilder {
    private final List<FormulaStep> steps = new ArrayList<>();
    private DefaultPipelineBuilder() {

    }

    public static DefaultPipelineBuilder builder() {
        return new DefaultPipelineBuilder();
    }

    public DefaultPipelineBuilder addStep(FormulaStep step) {
        steps.add(Objects.requireNonNull(step, "step"));
        return this;
    }

    public DefaultPipelineBuilder addSteps(List<FormulaStep> additional) {
        Objects.requireNonNull(additional, "additional");
        for (FormulaStep step : additional) {
            addStep(step);
        }
        return this;
    }

    private Function<FormulaParams, FormulaParams> compose() {
        Function<FormulaParams, FormulaParams> chain = Function.identity();
        for (FormulaStep step : steps) {
            chain = chain.andThen(step);
        }
        return chain;
    }

    public PreparedFormula build(Terminal terminal) {
        Objects.requireNonNull(terminal, "terminal");
        return new PreparedFormula(compose(), terminal);
    }

    public static final class Registry {
        private final Map<String, PreparedFormula> registry = new HashMap<>();

        public void register(String name, PreparedFormula formula) {
            registry.put(Objects.requireNonNull(name, "name"), Objects.requireNonNull(formula, "formula"));
        }

        public PreparedFormula get(String name) {
            return registry.get(name);
        }
    }
}
