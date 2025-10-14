package teamnova.omok.modules.state_machine.models;

import teamnova.omok.modules.state_machine.interfaces.BaseState;

import java.util.Objects;

public record StateStep(StateName nextState) {
    public static StateStep stay() {
        return new StateStep(null);
    }

    public static StateStep transition(StateName nextState) {
        Objects.requireNonNull(nextState, "nextState");
        return new StateStep(nextState);
    }

    public boolean hasTransition() {
        return nextState != null;
    }
}
