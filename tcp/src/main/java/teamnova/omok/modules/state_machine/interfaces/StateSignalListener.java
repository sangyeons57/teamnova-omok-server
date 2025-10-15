package teamnova.omok.modules.state_machine.interfaces;

import java.util.Set;
import teamnova.omok.modules.state_machine.models.LifecycleEventKind;
import teamnova.omok.modules.state_machine.models.StateName;

/**
 * Context-less unified lifecycle listener. External listeners receive only
 * the active state and lifecycle event kind. They can optionally filter
 * by states/events by returning a non-null set from states()/events().
 */
public interface StateSignalListener {
    default Set<StateName> states() { return null; } // null = all states
    default Set<LifecycleEventKind> events() { return null; } // null = all lifecycle kinds

    void onSignal(StateName state, LifecycleEventKind kind);
}
