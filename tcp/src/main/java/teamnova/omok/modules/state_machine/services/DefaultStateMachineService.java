package teamnova.omok.modules.state_machine.services;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

import teamnova.omok.modules.state_machine.interfaces.BaseEvent;
import teamnova.omok.modules.state_machine.interfaces.BaseState;
import teamnova.omok.modules.state_machine.interfaces.StateSignalListener;
import teamnova.omok.modules.state_machine.interfaces.StateContext;
import teamnova.omok.modules.state_machine.interfaces.StateMachineService;
import teamnova.omok.modules.state_machine.models.EventName;
import teamnova.omok.modules.state_machine.models.LifecycleEventKind;
import teamnova.omok.modules.state_machine.models.StateName;
import teamnova.omok.modules.state_machine.models.StateStep;

public class DefaultStateMachineService implements StateMachineService {

    private final Map<StateName, BaseState> states;
    private final Queue<PendingEvent> eventQueue;
    private final List<StateSignalListener> signalListeners;
    private BaseState currentState;

    public DefaultStateMachineService() {
        this.states = new java.util.concurrent.ConcurrentHashMap<>();
        this.eventQueue = new java.util.concurrent.ConcurrentLinkedQueue<>();
        this.signalListeners = new CopyOnWriteArrayList<>();
        this.currentState = null;
    }

    @Override
    public <I extends StateContext> void start(StateName stateName, I context) {
        Objects.requireNonNull(stateName, "stateName");
        Objects.requireNonNull(context, "context");
        currentState = states.get(stateName);
        if (currentState == null) {
            throw new IllegalStateException("No state registered for type " + stateName);
        }
        try {
            StateStep entryStep = currentState.onEnter(context);
            notifySignal(currentState.name(), LifecycleEventKind.ON_START);
            applyTransition(entryStep, context);
        } catch (Throwable t) {
            System.err.println("[STATE-MACHINE] onEnter failed during start for state=" + currentState.name().name() + ": " + t);
            t.printStackTrace();
            throw t;
        }
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
    public void submit(BaseEvent event) {
        Objects.requireNonNull(event, "event");
        eventQueue.offer(new PendingEvent(event));
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
        try {
            StateStep updateStep = currentState.onUpdate(context, now);
            notifySignal(currentState.name(), LifecycleEventKind.ON_UPDATE);
            applyTransition(updateStep, context);
        } catch (Throwable t) {
            System.err.println("[STATE-MACHINE] onUpdate failed for state=" + currentState.name().name() + ": " + t);
            t.printStackTrace();
            throw t;
        }
    }

    private void processPending(PendingEvent pending, StateContext context) {
        if (currentState == null) {
            return;
        }
        try {
            StateStep eventStep = currentState.onEvent(context, pending.event());
            // No outbound signal for external input event to avoid cycles
            applyTransition(eventStep, context);
        } catch (Throwable t) {
            System.err.println("[STATE-MACHINE] onEvent failed for state=" + currentState.name().name() + 
                ", event=" + String.valueOf(pending.event()) + ": " + t);
            t.printStackTrace();
            throw t;
        }
    }

    private void applyTransition(StateStep step, StateContext context) {
        if (step != null && step.hasTransition()) {
            try {
                notifyTransition(step.nextState());
                transitionDirect(step.nextState(), context);
            } catch (Throwable t) {
                System.err.println("[STATE-MACHINE] applyTransition failed to nextState=" + step.nextState().name() + ": " + t);
                t.printStackTrace();
                throw t;
            }
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
        StateName from = currentState != null ? currentState.name() : null;
        if (currentState != null) {
            try {
                currentState.onExit(context);
                notifySignal(from, LifecycleEventKind.ON_EXIT);
            } catch (Throwable t) {
                System.err.println("[STATE-MACHINE] onExit failed for state=" + from.name() + ": " + t);
                t.printStackTrace();
                throw t;
            }
        }
        currentState = next;
        try {
            StateStep entryStep = currentState.onEnter(context);
            notifySignal(currentState.name(), LifecycleEventKind.ON_START);
            applyTransition(entryStep, context);
        } catch (Throwable t) {
            System.err.println("[STATE-MACHINE] onEnter failed for state=" + currentState.name().name() +
                (from != null ? " (from=" + from.name() + ")" : "") + ": " + t);
            t.printStackTrace();
            throw t;
        }
    }


    private void notifyTransition(StateName stateName) {
        if (currentState != null) {
            try {
                System.out.println("[STATE-MACHINE][transition] from=" + currentState.name().name() + " to=" + stateName.name());
            } catch (Throwable ignored) { }
            notifySignal(currentState.name(), LifecycleEventKind.ON_TRANSITION);
        }
    }

    private void notifySignal(StateName state, LifecycleEventKind kind) {
        for (StateSignalListener l : signalListeners) {
            Set<StateName> ss = l.states();
            if (ss != null && !ss.contains(state)) continue;
            Set<LifecycleEventKind> es = l.events();
            if (es != null && !es.contains(kind)) continue;
            l.onSignal(state, kind);
        }
    }

    private record PendingEvent(BaseEvent event) { }
}
