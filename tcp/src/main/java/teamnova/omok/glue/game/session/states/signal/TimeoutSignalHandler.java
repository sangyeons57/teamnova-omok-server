package teamnova.omok.glue.game.session.states.signal;

import java.util.Set;

import teamnova.omok.glue.game.session.states.manage.GameSessionStateContext;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateType;
import teamnova.omok.modules.state_machine.interfaces.StateSignalListener;
import teamnova.omok.modules.state_machine.models.LifecycleEventKind;
import teamnova.omok.modules.state_machine.models.StateName;

/**
 * Handles generic timeout-related housekeeping on lifecycle signals.
 */
public final class TimeoutSignalHandler implements StateSignalListener {
    private final GameSessionStateContext context;
    private final teamnova.omok.glue.game.session.model.dto.GameSessionServices services;

    public TimeoutSignalHandler(GameSessionStateContext context,
                                teamnova.omok.glue.game.session.model.dto.GameSessionServices services) {
        this.context = context;
        this.services = services;
    }

    @Override
    public Set<LifecycleEventKind> events() {
        return java.util.Set.of(LifecycleEventKind.ON_START);
    }

    @Override
    public Set<StateName> states() {
        return java.util.Set.of(
            GameSessionStateType.COMPLETED.toStateName()
        );
    }

    @Override
    public void onSignal(StateName state, LifecycleEventKind kind) {
        if (kind != LifecycleEventKind.ON_START) return;
        if (GameSessionStateType.stateNameLookup(state) == GameSessionStateType.COMPLETED) {
            services.turnTimeoutScheduler().cancel(context.session().sessionId());
            services.decisionTimeoutScheduler().cancel(context.session().sessionId());
        }
    }
}
