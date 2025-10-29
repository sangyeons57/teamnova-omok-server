package teamnova.omok.glue.game.session.states.state;

import java.util.Objects;

import teamnova.omok.glue.game.session.states.manage.GameSessionStateContext;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContextService;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateType;
import teamnova.omok.modules.state_machine.interfaces.BaseState;
import teamnova.omok.modules.state_machine.interfaces.StateContext;
import teamnova.omok.modules.state_machine.models.StateName;
import teamnova.omok.modules.state_machine.models.StateStep;

/**
 * Marks the start of a new overall turn cycle and prepares runtime buffers.
 */
public final class TurnStartState implements BaseState {
    private final GameSessionStateContextService contextService;

    public TurnStartState(GameSessionStateContextService contextService) {
        this.contextService = Objects.requireNonNull(contextService, "contextService");
    }

    @Override
    public StateName name() {
        return GameSessionStateType.TURN_START.toStateName();
    }

    @Override
    public <I extends StateContext> StateStep onEnter(I context) {
        GameSessionStateContext ctx = (GameSessionStateContext) context;
        // Prepare personal turn frames and move to the first personal turn of this round
        contextService.turn().resetPersonalTurns(ctx);
        return StateStep.transition(GameSessionStateType.TURN_PERSONAL_START.toStateName());
    }
}
