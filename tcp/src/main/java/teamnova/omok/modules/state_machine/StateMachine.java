package teamnova.omok.modules.state_machine;

import teamnova.omok.modules.state_machine.interfaces.StateMachineManager;
import teamnova.omok.modules.state_machine.services.DefaultStateMachineManager;

public class StateMachine {
    public static StateMachineManager createStateMatchingManager(){
        return new DefaultStateMachineManager();
    }
}
