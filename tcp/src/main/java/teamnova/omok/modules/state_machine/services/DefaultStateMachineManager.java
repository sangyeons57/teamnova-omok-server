package teamnova.omok.modules.state_machine.services;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

import teamnova.omok.modules.state_machine.interfaces.BaseEvent;
import teamnova.omok.modules.state_machine.interfaces.BaseState;
import teamnova.omok.modules.state_machine.interfaces.StateLifecycleListener;
import teamnova.omok.modules.state_machine.interfaces.StateSignalListener;
import teamnova.omok.modules.state_machine.interfaces.StateContext;
import teamnova.omok.modules.state_machine.interfaces.StateMachineManager;
import teamnova.omok.modules.state_machine.models.LifecycleEventKind;
import teamnova.omok.modules.state_machine.models.StateName;
import teamnova.omok.modules.state_machine.models.StateStep;

public class DefaultStateMachineManager implements StateMachineManager {

    private final Map<StateName, BaseState> states;
    private final Queue<PendingEvent> eventQueue;
    private final Map<StateName, List<StateLifecycleListener>> lifecycleListeners; // deprecated path
    private final List<StateSignalListener> signalListeners;
    private BaseState currentState;
    private Consumer<StateName> transitionListener;

    public DefaultStateMachineManager() {
        this.states = new java.util.concurrent.ConcurrentHashMap<>();
        this.eventQueue = new java.util.concurrent.ConcurrentLinkedQueue<>();
        this.lifecycleListeners = new java.util.concurrent.ConcurrentHashMap<>();
        this.signalListeners = new CopyOnWriteArrayList<>();
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
        notifyEnter(currentState.name(), context);
        notifySignal(currentState.name(), LifecycleEventKind.ON_START);
        notifySignal(currentState.name(), LifecycleEventKind.ON_TRANSITION); // initial as transition into first state
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
    public void onStateLifecycle(StateLifecycleListener listener) {
        if (listener != null && listener.state() != null) {
            lifecycleListeners
                .computeIfAbsent(listener.state(), k -> new CopyOnWriteArrayList<>())
                .add(listener);
        }
    }

    @Override
    public void addStateSignalListener(StateSignalListener listener) {
        if (listener != null) {
            signalListeners.add(listener);
        }
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
        notifyUpdate(currentState.name(), context, now);
        notifySignal(currentState.name(), LifecycleEventKind.ON_UPDATE);
        applyTransition(updateStep, context);
    }

    private void processPending(PendingEvent pending, StateContext context) {
        if (currentState == null) {
            return;
        }
        StateStep eventStep = currentState.onEvent(context, pending.event());
        // No outbound signal for external input event to avoid cycles
        notifyEvent(currentState.name(), context, pending.event()); // deprecated path only
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
            StateName from = currentState.name();
            currentState.onExit(context);
            notifyExit(from, context);
            notifySignal(from, LifecycleEventKind.ON_EXIT);
        }
        currentState = next;
        StateStep entryStep = currentState.onEnter(context);
        notifyEnter(currentState.name(), context);
        notifySignal(currentState.name(), LifecycleEventKind.ON_START);
        notifySignal(currentState.name(), LifecycleEventKind.ON_TRANSITION);
        notifyTransition(stateName);
        applyTransition(entryStep, context);
    }

    private void notifyEnter(StateName state, StateContext ctx) {
        var ls = lifecycleListeners.get(state);
        if (ls == null) return;
        for (StateLifecycleListener l : ls) {
            try { l.onEnter(ctx); } catch (Throwable t) { }
        }
    }

    private void notifyExit(StateName state, StateContext ctx) {
        var ls = lifecycleListeners.get(state);
        if (ls == null) return;
        for (StateLifecycleListener l : ls) {
            try { l.onExit(ctx); } catch (Throwable t) { }
        }
    }

    private void notifyUpdate(StateName state, StateContext ctx, long now) {
        var ls = lifecycleListeners.get(state);
        if (ls == null) return;
        for (StateLifecycleListener l : ls) {
            try { l.onUpdate(ctx, now); } catch (Throwable t) { }
        }
    }

    private void notifyEvent(StateName state, StateContext ctx, BaseEvent ev) {
        var ls = lifecycleListeners.get(state);
        if (ls == null) return;
        for (StateLifecycleListener l : ls) {
            try { l.onEvent(ctx, ev); } catch (Throwable t) { }
        }
    }

    private void notifyTransition(StateName stateName) {
        transitionListener.accept(stateName);
    }

    private void notifySignal(StateName state, LifecycleEventKind kind) {
        for (StateSignalListener l : signalListeners) {
            try {
                var ss = l.states();
                if (ss != null && !ss.contains(state)) continue;
                var es = l.events();
                if (es != null && !es.contains(kind)) continue;
                l.onSignal(state, kind);
            } catch (Throwable t) { }
        }
    }

    private record PendingEvent(BaseEvent event, Consumer<StateContext> callback) { }
}
