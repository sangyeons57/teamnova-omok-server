package teamnova.omok.modules.state_machine.interfaces;

import java.util.function.Consumer;

import teamnova.omok.modules.state_machine.models.StateName;

public interface StateMachineManager {

    <I extends StateContext> void start(StateName stateName, I context);

    void registerState(BaseState state);

    StateName currentState();

    default void submit(BaseEvent event) {
        submit(event, null);
    }

    void submit(BaseEvent event, Consumer<StateContext> callback);

    default void onTransition(Consumer<StateName> listener) { }

    // State-scoped lifecycle listener channel
    default void onStateLifecycle(StateLifecycleListener listener) { }

    void process(StateContext stateContext, long now);
}
