package teamnova.omok.modules.state_machine;

import java.util.Objects;
import java.util.function.Consumer;

import teamnova.omok.modules.state_machine.interfaces.BaseEvent;
import teamnova.omok.modules.state_machine.interfaces.BaseState;
import teamnova.omok.modules.state_machine.interfaces.StateContext;
import teamnova.omok.modules.state_machine.interfaces.StateMachineManager;
import teamnova.omok.modules.state_machine.models.StateName;
import teamnova.omok.modules.state_machine.services.DefaultStateMachineManager;

/**
 * Single entry point used to work with the state machine module.
 * Encapsulates the underlying manager instance so that all callers
 * interact through a controlled facade.
 */
public final class StateMachineGateway {
    private StateMachineGateway() { }

    public static Handle open() {
        return new Handle(new DefaultStateMachineManager());
    }

    public static Handle wrap(StateMachineManager manager) {
        return new Handle(manager);
    }

    public static final class Handle {
        private final StateMachineManager delegate;

        private Handle(StateMachineManager delegate) {
            this.delegate = Objects.requireNonNull(delegate, "delegate");
        }

        public Handle register(BaseState state) {
            delegate.registerState(Objects.requireNonNull(state, "state"));
            return this;
        }

        public Handle onTransition(Consumer<StateName> listener) {
            delegate.onTransition(listener);
            return this;
        }

        public <I extends StateContext> Handle start(StateName stateName, I context) {
            delegate.start(Objects.requireNonNull(stateName, "stateName"),
                Objects.requireNonNull(context, "context"));
            return this;
        }

        public StateName currentState() {
            return delegate.currentState();
        }

        public void submit(BaseEvent event) {
            delegate.submit(Objects.requireNonNull(event, "event"));
        }

        public void submit(BaseEvent event, Consumer<StateContext> callback) {
            Objects.requireNonNull(event, "event");
            delegate.submit(event, callback);
        }

        public void process(StateContext context, long now) {
            delegate.process(Objects.requireNonNull(context, "context"), now);
        }
    }
}

