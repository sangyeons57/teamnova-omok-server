package teamnova.omok.glue.game.session.states.state;

import java.util.Objects;

import teamnova.omok.glue.game.session.model.dto.GameSessionServices;
import teamnova.omok.glue.game.session.model.vo.StonePlacementMetadata;
import teamnova.omok.glue.game.session.services.RuleService;
import teamnova.omok.glue.game.session.services.RuleTurnStateView;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContext;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContextService;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateType;
import teamnova.omok.glue.game.session.states.manage.TurnCycleContext;
import teamnova.omok.glue.rule.RuleRuntimeContext;
import teamnova.omok.glue.rule.RuleTriggerKind;
import teamnova.omok.modules.state_machine.interfaces.BaseState;
import teamnova.omok.modules.state_machine.interfaces.StateContext;
import teamnova.omok.modules.state_machine.models.StateName;
import teamnova.omok.modules.state_machine.models.StateStep;

/**
 * Writes the validated stone onto the board.
 */
public final class MoveApplyingState implements BaseState {
    private final GameSessionStateContextService contextService;
    private final GameSessionServices services;

    public MoveApplyingState(GameSessionStateContextService contextService,
                             GameSessionServices services) {
        this.contextService = Objects.requireNonNull(contextService, "contextService");
        this.services = Objects.requireNonNull(services, "services");
    }
    @Override
    public StateName name() {
        return GameSessionStateType.MOVE_APPLYING.toStateName();
    }

    @Override
    public <I extends StateContext> StateStep onEnter(I context) {
        return onEnterInternal((GameSessionStateContext) context);
    }

    private StateStep onEnterInternal(GameSessionStateContext context) {
        TurnCycleContext cycle = contextService.turn().activeTurnCycle(context);
        if (cycle == null) {
            return StateStep.transition(GameSessionStateType.TURN_WAITING.toStateName());
        }
        fireRules(context, RuleTriggerKind.PRE_PLACEMENT);

        int x = cycle.x();
        int y = cycle.y();
        if (!services.boardService().isWithinBounds(context.board(), x, y)
            || !services.boardService().isEmpty(context.board(), x, y)) {
            // fallback to original validated location if rule chose an invalid spot
            cycle.updatePosition(cycle.originalX(), cycle.originalY());
            x = cycle.x();
            y = cycle.y();
        }

        int playerIndex = context.participants().playerIndexOf(cycle.userId());
        StonePlacementMetadata metadata;
        if (cycle.snapshots().current() != null) {
            metadata = StonePlacementMetadata.forPlayer(
                cycle.snapshots().current(),
                cycle.userId(),
                playerIndex
            );
        } else {
            metadata = StonePlacementMetadata.empty();
        }
        services.boardService().setStone(context.board(), x, y, cycle.stone(), metadata);
        fireRules(context, RuleTriggerKind.POST_PLACEMENT);
        return StateStep.transition(GameSessionStateType.OUTCOME_EVALUATING.toStateName());
    }

    private void fireRules(GameSessionStateContext context, RuleTriggerKind triggerKind) {
        RuleTurnStateView view = RuleTurnStateView.capture(context, services.turnService());
        RuleRuntimeContext runtime = new RuleRuntimeContext(
            services,
            contextService,
            context,
            view,
            triggerKind
        );
        RuleService.getInstance().activateRules(context.rules(), runtime);
    }
}
