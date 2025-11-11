package teamnova.omok.modules.state_machine.interfaces;

import teamnova.omok.modules.state_machine.models.StateName;
import teamnova.omok.modules.state_machine.models.StateStep;

public interface BaseState {

    StateName name();

    default <I extends StateContext> StateStep onEnter(I context) {
        return StateStep.stay();
    }

    default <I extends StateContext> void onExit(I context) { }

    default <I extends StateContext> StateStep onUpdate(I context, long now) {
        return StateStep.stay();
    }

    default <I extends StateContext> StateStep onEvent(I context, BaseEvent event) {
        return StateStep.stay();
    }
}
