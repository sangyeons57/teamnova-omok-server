package teamnova.omok.glue.game.session.states.signal;

import java.util.Set;

import teamnova.omok.glue.game.session.log.GameSessionLogger;
import teamnova.omok.glue.game.session.states.GameStateHub;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContext;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateType;
import teamnova.omok.modules.state_machine.interfaces.StateSignalListener;
import teamnova.omok.modules.state_machine.models.LifecycleEventKind;
import teamnova.omok.modules.state_machine.models.StateName;

/**
 * Extracted lifecycle logging signal handler.
 * Mirrors the previous anonymous listener in GameStateHub, handling:
 * - signal logging for all lifecycle kinds
 * - enter/exit logging
 * - transition logging and currentType update on ON_START
 */
public final class LifecycleLoggingSignalHandler implements StateSignalListener {
    private final GameStateHub hub;
    private final GameSessionStateContext context;

    public LifecycleLoggingSignalHandler(GameStateHub hub, GameSessionStateContext context) {
        this.hub = hub;
        this.context = context;
    }

    @Override
    public Set<LifecycleEventKind> events() {
        return java.util.Set.of(
            LifecycleEventKind.ON_TRANSITION,
            LifecycleEventKind.ON_START,
            LifecycleEventKind.ON_EXIT,
            LifecycleEventKind.ON_UPDATE
        );
    }

    @Override
    public void onSignal(StateName state, LifecycleEventKind kind) {
        GameSessionStateType type = GameSessionStateType.stateNameLookup(state);
        if (type != null) {
            GameSessionLogger.signal(context, type, kind);
        }
        if (kind == LifecycleEventKind.ON_EXIT) {
            if (type != null) {
                GameSessionLogger.exit(context, type);
            }
            return;
        }
        if (kind == LifecycleEventKind.ON_START) {
            GameSessionStateType to = GameSessionStateType.stateNameLookup(state);
            if (to == null) {
                throw new IllegalStateException("Unrecognised state: " + state.name());
            }
            GameSessionStateType from = hub.currentType();
            if (to != from) {
                GameSessionLogger.transition(context, from, to, "state-machine");
                GameSessionLogger.enter(context, to);
                hub.updateCurrentType(to);
            }
        }
    }
}
