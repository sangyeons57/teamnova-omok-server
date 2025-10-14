package teamnova.omok.modules.state_machine.models;

import teamnova.omok.modules.state_machine.interfaces.BaseState;

public class StartState implements BaseState {
    @Override
    public StateName name() {
        return StateName.of("start");
    }
}
