package teamnova.omok.modules.state_machine.interfaces;

import teamnova.omok.modules.state_machine.interfaces.BaseEvent;
import teamnova.omok.modules.state_machine.interfaces.StateContext;
import teamnova.omok.modules.state_machine.models.StateName;

/**
 * State-scoped lifecycle listener bound 1:1 to a specific state.
 * Called by the manager when that state is active.
 */
public interface StateLifecycleListener {
    StateName state();

    default <I extends StateContext> void onEnter(I context) { }
    default <I extends StateContext> void onExit(I context) { }
    default <I extends StateContext> void onUpdate(I context, long now) { }
    default <I extends StateContext> void onEvent(I context, BaseEvent event) { }
}
