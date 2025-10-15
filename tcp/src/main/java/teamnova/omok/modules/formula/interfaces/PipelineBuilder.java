package teamnova.omok.modules.formula.interfaces;

import teamnova.omok.modules.formula.models.PreparedFormula;
import teamnova.omok.modules.formula.services.DefaultPipelineBuilder;
import teamnova.omok.modules.formula.steps.BootstrapStep;
import teamnova.omok.modules.formula.steps.ClampWinResultStep;
import teamnova.omok.modules.formula.steps.DisconnectedPenaltyStep;
import teamnova.omok.modules.formula.steps.ResolveOutcomeStep;

import java.util.List;
import java.util.Objects;

public interface PipelineBuilder {

    DefaultPipelineBuilder addStep(FormulaStep step) ;
    DefaultPipelineBuilder addSteps(List<FormulaStep> additional);

    PreparedFormula build(Terminal terminal);

    default PipelineBuilder bootstrap (double stageRequirementPoints, double stageRequirementWins, double initialBonusStageLimit, double disconnectedPenalty ){
        return addStep(new BootstrapStep(stageRequirementPoints, stageRequirementWins, initialBonusStageLimit, disconnectedPenalty));
    }

    // The fourth parameter is the loss ratio divisor for defeats (e.g., 500)
    default PipelineBuilder resolveOutcome (double stageRequirementPoints, double stageRequirementWins, double initialBonusStageLimit, double lossRatioDivisor){
        return addStep(new ResolveOutcomeStep(stageRequirementPoints, stageRequirementWins, initialBonusStageLimit, lossRatioDivisor));
    }

    default PipelineBuilder clampWinResult (double stageRequirementPoints, double stageRequirementWins) {
        return addStep(new ClampWinResultStep(stageRequirementPoints, stageRequirementWins));
    }

    default PipelineBuilder applyDisconnectedPenalty(double disconnectedPenalty) {
        return addStep(new DisconnectedPenaltyStep(disconnectedPenalty));
    }
}
