package teamnova.omok.modules.state_machine.interfaces;

import teamnova.omok.modules.state_machine.models.StateName;

public interface StateMachineService {

    <I extends StateContext> void start(StateName stateName, I context);

    void registerState(BaseState state);

    StateName currentState();

    void submit(BaseEvent event);

    // Unified signal listener (context-less)
    default void addStateSignalListener(StateSignalListener listener) { }

    void process(StateContext stateContext, long now);
}
