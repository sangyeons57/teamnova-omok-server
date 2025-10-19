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
 * Ensures decision timers are cleared before the session is fully terminated.
 */
public final class SessionTerminatingState implements BaseState {
    private final GameSessionStateContextService contextService;

    public SessionTerminatingState(GameSessionStateContextService contextService) {
        this.contextService = Objects.requireNonNull(contextService, "contextService");
    }
    @Override
    public StateName name() {
        return GameSessionStateType.SESSION_TERMINATING.toStateName();
    }

    @Override
    public <I extends StateContext> StateStep onEnter(I context) {
        return onEnterInternal((GameSessionStateContext) context);
    }

    private StateStep onEnterInternal(GameSessionStateContext context) {
        contextService.postGame().clearDecisionDeadline(context);
        return StateStep.transition(GameSessionStateType.COMPLETED.toStateName());
    }
}
