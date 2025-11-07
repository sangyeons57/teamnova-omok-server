package teamnova.omok.glue.game.session.states.state;

import java.util.Objects;

import teamnova.omok.glue.game.session.model.GameSession;
import teamnova.omok.glue.game.session.model.dto.GameSessionServices;
import teamnova.omok.glue.game.session.services.GameSessionRematchService;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContext;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContextService;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateType;
import teamnova.omok.modules.state_machine.interfaces.BaseState;
import teamnova.omok.modules.state_machine.interfaces.StateContext;
import teamnova.omok.modules.state_machine.models.StateName;
import teamnova.omok.modules.state_machine.models.StateStep;

/**
 * Finalizes the current session before rematch participants move to a new session.
 */
public final class SessionRematchPreparingState implements BaseState {
    private final GameSessionStateContextService contextService;
    private final GameSessionServices services;

    public SessionRematchPreparingState(GameSessionStateContextService contextService,
                                        GameSessionServices services) {
        this.contextService = Objects.requireNonNull(contextService, "contextService");
        this.services = Objects.requireNonNull(services, "services");
    }
    @Override
    public StateName name() {
        return GameSessionStateType.SESSION_REMATCH_PREPARING.toStateName();
    }

    @Override
    public <I extends StateContext> StateStep onEnter(I context) {
        return onEnterInternal((GameSessionStateContext) context);
    }

    private StateStep onEnterInternal(GameSessionStateContext context) {
        GameSession rematch = contextService.postGame().consumePendingRematchSession(context);
        if (rematch != null) {
            GameSessionRematchService.finalizeAndJoin(services, rematch);
        }
        contextService.postGame().clearDecisionDeadline(context);
        return StateStep.transition(GameSessionStateType.COMPLETED.toStateName());
    }
}
