package teamnova.omok.glue.game.session.states.state;

import java.util.Objects;

import teamnova.omok.glue.game.session.model.dto.GameSessionServices;
import teamnova.omok.glue.game.session.interfaces.GameTurnService;
import teamnova.omok.glue.game.session.model.dto.TurnSnapshot;
import teamnova.omok.glue.game.session.model.runtime.TurnPersonalFrame;
import teamnova.omok.glue.game.session.model.result.MoveStatus;
import teamnova.omok.glue.game.session.services.RuleService;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContext;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContextService;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateType;
import teamnova.omok.glue.rule.RuleRuntimeContext;
import teamnova.omok.glue.rule.RuleTriggerKind;
import teamnova.omok.modules.state_machine.interfaces.BaseState;
import teamnova.omok.modules.state_machine.interfaces.StateContext;
import teamnova.omok.modules.state_machine.models.StateName;
import teamnova.omok.modules.state_machine.models.StateStep;

/**
 * Handles completion of the active player's turn and prepares information for subsequent stages.
 */
public final class TurnPersonalCompletedState implements BaseState {
    private final GameSessionStateContextService contextService;
    private final GameSessionServices services;
    private final GameTurnService turnService;

    public TurnPersonalCompletedState(GameSessionStateContextService contextService,
                                      GameSessionServices services) {
        this.contextService = Objects.requireNonNull(contextService, "contextService");
        this.services = Objects.requireNonNull(services, "services");
        this.turnService = Objects.requireNonNull(services.turnService(), "turnService");
    }

    @Override
    public StateName name() {
        return GameSessionStateType.TURN_PERSONAL_COMPLETED.toStateName();
    }

    @Override
    public <I extends StateContext> StateStep onEnter(I context) {
        return onEnterInternal((GameSessionStateContext) context);
    }

    private StateStep onEnterInternal(GameSessionStateContext context) {
        TurnPersonalFrame frame = contextService.turn().currentPersonalTurn(context);
        if (frame == null || !frame.hasActiveMove()) {
            return StateStep.transition(GameSessionStateType.TURN_WAITING.toStateName());
        }
        TurnSnapshot nextSnapshot = turnService.advanceSkippingDisconnected(
            context.turns(),
            context.participants().disconnectedUsersView(),
            frame.requestedAtMillis()
        );
        frame.nextSnapshot(nextSnapshot);
        contextService.turn().recordTurnSnapshot(context, nextSnapshot, frame.requestedAtMillis());
        fireRules(context, RuleTriggerKind.TURN_ADVANCE, nextSnapshot);
        contextService.turn().finalizeMoveOutcome(context, MoveStatus.SUCCESS);
        contextService.turn().clearTurnCycle(context);
        GameSessionStateType nextState = nextSnapshot != null && nextSnapshot.wrapped()
            ? GameSessionStateType.TURN_ROUND_COMPLETED
            : GameSessionStateType.TURN_PERSONAL_STARTING;
        return StateStep.transition(nextState.toStateName());
    }

    private void fireRules(GameSessionStateContext context,
                           RuleTriggerKind trigger,
                           TurnSnapshot snapshot) {
        RuleRuntimeContext runtime = new RuleRuntimeContext(
            services,
            contextService,
            context,
            snapshot,
            trigger
        );
        RuleService.getInstance().activateRules(context.rules(), runtime);
    }
}
