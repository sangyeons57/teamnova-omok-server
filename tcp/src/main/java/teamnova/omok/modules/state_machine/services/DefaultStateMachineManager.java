package teamnova.omok.modules.state_machine.services;

import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.function.Consumer;

import teamnova.omok.modules.state_machine.interfaces.BaseEvent;
import teamnova.omok.modules.state_machine.interfaces.BaseState;
import teamnova.omok.modules.state_machine.interfaces.StateContext;
import teamnova.omok.modules.state_machine.interfaces.StateMachineManager;
import teamnova.omok.modules.state_machine.models.StateName;
import teamnova.omok.modules.state_machine.models.StateStep;

public class DefaultStateMachineManager implements StateMachineManager {
    private final Map<StateName, BaseState> states;
    private final Queue<PendingEvent> eventQueue;
    private BaseState currentState;
    private Consumer<StateName> transitionListener;

    public DefaultStateMachineManager() {
        this.states = new java.util.concurrent.ConcurrentHashMap<>();
        this.eventQueue = new java.util.concurrent.ConcurrentLinkedQueue<>();
        this.currentState = null;
        this.transitionListener = stateName -> { };
    }

    @Override
    public <I extends StateContext> void start(StateName stateName, I context) {
        Objects.requireNonNull(stateName, "stateName");
        Objects.requireNonNull(context, "context");
        currentState = states.get(stateName);
        if (currentState == null) {
            throw new IllegalStateException("No state registered for type " + stateName);
        }
        StateStep entryStep = currentState.onEnter(context);
        notifyTransition(currentState.name());
        applyTransition(entryStep, context);
    }

    @Override
    public void registerState(BaseState state) {
        Objects.requireNonNull(state, "state");
        states.put(state.name(), state);
    }

    @Override
    public StateName currentState() {
        return currentState != null ? currentState.name() : null;
    }

    @Override
    public void submit(BaseEvent event, Consumer<StateContext> callback) {
        Objects.requireNonNull(event, "event");
        eventQueue.offer(new PendingEvent(event, callback));
    }

    @Override
    public void onTransition(Consumer<StateName> listener) {
        this.transitionListener = listener != null ? listener : stateName -> { };
    }

    @Override
    public void process(StateContext context, long now) {
        Objects.requireNonNull(context, "context");
        PendingEvent pending;
        while ((pending = eventQueue.poll()) != null) {
            processPending(pending, context);
        }
        if (currentState == null) {
            return;
        }
        StateStep updateStep = currentState.onUpdate(context, now);
        applyTransition(updateStep, context);
    }

    private void processPending(PendingEvent pending, StateContext context) {
        if (currentState == null) {
            return;
        }
        StateStep eventStep = currentState.onEvent(context, pending.event());
        applyTransition(eventStep, context);
        Consumer<StateContext> callback = pending.callback();
        if (callback != null) {
            callback.accept(context);
        }
    }

    private void applyTransition(StateStep step, StateContext context) {
        if (step != null && step.hasTransition()) {
            transitionDirect(step.nextState(), context);
        }
    }

    private void transitionDirect(StateName stateName, StateContext context) {
        BaseState next = states.get(stateName);
        if (next == null) {
            throw new IllegalStateException("No state registered for type " + stateName);
        }
        if (next == currentState) {
            return;
        }
        if (currentState != null) {
            currentState.onExit(context);
        }
        currentState = next;
        StateStep entryStep = currentState.onEnter(context);
        notifyTransition(stateName);
        applyTransition(entryStep, context);
    }

    private void notifyTransition(StateName stateName) {
        transitionListener.accept(stateName);
    }

    private record PendingEvent(BaseEvent event, Consumer<StateContext> callback) { }
}
