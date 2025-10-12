package teamnova.omok.state.game.manage;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

import teamnova.omok.service.BoardService;
import teamnova.omok.service.OutcomeService;
import teamnova.omok.service.TurnService;
import teamnova.omok.state.game.contract.GameSessionState;
import teamnova.omok.state.game.event.GameSessionEvent;
import teamnova.omok.state.game.event.GameSessionEventRegistry;
import teamnova.omok.state.game.event.GameSessionEventRegistry.HandlerEntry;
import teamnova.omok.state.game.event.GameSessionEventType;
import teamnova.omok.state.game.state.CompletedGameSessionState;
import teamnova.omok.state.game.state.LobbyGameSessionState;
import teamnova.omok.state.game.state.MoveApplyingState;
import teamnova.omok.state.game.state.MoveValidatingState;
import teamnova.omok.state.game.state.OutcomeEvaluatingState;
import teamnova.omok.state.game.state.PostGameDecisionResolvingState;
import teamnova.omok.state.game.state.PostGameDecisionWaitingState;
import teamnova.omok.state.game.state.SessionRematchPreparingState;
import teamnova.omok.state.game.state.SessionTerminatingState;
import teamnova.omok.state.game.state.TurnFinalizingState;
import teamnova.omok.state.game.state.TurnWaitingState;
import teamnova.omok.store.GameSession;

/**
 * Maintains the active state for a {@link GameSession} and routes lifecycle events.
 */
public class GameSessionStateManager {
    private final GameSessionStateContext context;
    private final Map<GameSessionStateType, StateRegistration> registrations =
        new EnumMap<>(GameSessionStateType.class);
    private StateRegistration currentRegistration;
    private final Queue<PendingEvent> eventQueue = new ConcurrentLinkedQueue<>();

    public GameSessionStateManager(GameSession session,
                                   BoardService boardService,
                                   TurnService turnService,
                                   OutcomeService outcomeService) {
        Objects.requireNonNull(session, "session");
        this.context = new GameSessionStateContext(session, boardService, turnService, outcomeService);
        registerState(GameSessionStateType.LOBBY, new LobbyGameSessionState());
        registerState(GameSessionStateType.TURN_WAITING, new TurnWaitingState());
        registerState(GameSessionStateType.MOVE_VALIDATING, new MoveValidatingState());
        registerState(GameSessionStateType.MOVE_APPLYING, new MoveApplyingState());
        registerState(GameSessionStateType.OUTCOME_EVALUATING, new OutcomeEvaluatingState());
        registerState(GameSessionStateType.TURN_FINALIZING, new TurnFinalizingState());
        registerState(GameSessionStateType.POST_GAME_DECISION_WAITING, new PostGameDecisionWaitingState());
        registerState(GameSessionStateType.POST_GAME_DECISION_RESOLVING, new PostGameDecisionResolvingState());
        registerState(GameSessionStateType.SESSION_REMATCH_PREPARING, new SessionRematchPreparingState());
        registerState(GameSessionStateType.SESSION_TERMINATING, new SessionTerminatingState());
        registerState(GameSessionStateType.COMPLETED, new CompletedGameSessionState());
        this.currentRegistration = registrations.get(GameSessionStateType.LOBBY);
        if (currentRegistration == null) {
            throw new IllegalStateException("Lobby state not registered");
        }
        GameSessionStateStep entryStep = currentRegistration.state.onEnter(context);
        applyTransition(entryStep);
    }

    private void registerState(GameSessionStateType type, GameSessionState state) {
        if (state.type() != type) {
            throw new IllegalArgumentException("State type mismatch: expected " + type + " but was " + state.type());
        }
        GameSessionEventRegistry registry = new GameSessionEventRegistry();
        state.registerHandlers(registry);
        registrations.put(type, new StateRegistration(state, registry.handlers()));
    }

    public GameSessionStateType currentType() {
        return currentRegistration.state.type();
    }

    public GameSession session() {
        return context.session();
    }

    public void submit(GameSessionEvent event, Consumer<GameSessionStateContext> callback) {
        Objects.requireNonNull(event, "event");
        eventQueue.add(new PendingEvent(event, callback));
    }

    public void process(long now) {
        PendingEvent pending;
        while ((pending = eventQueue.poll()) != null) {
            processPending(pending);
        }
        GameSessionStateStep updateStep = currentRegistration.state.onUpdate(context, now);
        applyTransition(updateStep);
    }

    private void processPending(PendingEvent pending) {
        handleEvent(pending.event);
        if (pending.callback != null) {
            pending.callback.accept(context);
        }
    }

    private GameSessionStateStep handleEvent(GameSessionEvent event) {
        Objects.requireNonNull(event, "event");
        HandlerEntry entry = currentRegistration.handlers.get(event.type());
        GameSessionStateStep step = (entry != null)
            ? invokeHandler(entry, event)
            : GameSessionStateStep.stay();
        applyTransition(step);
        return step;
    }

    private GameSessionStateStep invokeHandler(HandlerEntry entry, GameSessionEvent event) {
        return entry.invoke(context, event);
    }

    private void applyTransition(GameSessionStateStep step) {
        if (step != null && step.hasTransition()) {
            transitionDirect(step.nextState());
        }
    }

    private void transitionDirect(GameSessionStateType targetType) {
        StateRegistration next = registrations.get(targetType);
        if (next == null) {
            throw new IllegalStateException("No state registered for type " + targetType);
        }
        if (next == currentRegistration) {
            return;
        }
        currentRegistration.state.onExit(context);
        currentRegistration = next;
        GameSessionStateStep entryStep = currentRegistration.state.onEnter(context);

        context.

        applyTransition(entryStep);
    }

    private record StateRegistration(GameSessionState state,
                                     Map<GameSessionEventType, HandlerEntry> handlers) { }

    private record PendingEvent(GameSessionEvent event,
                                Consumer<GameSessionStateContext> callback) { }
}
