package teamnova.omok.glue.state.game;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import teamnova.omok.glue.rule.RulesContext;
import teamnova.omok.glue.service.BoardService;
import teamnova.omok.glue.service.ScoreService;
import teamnova.omok.glue.service.TurnService;
import teamnova.omok.glue.state.game.manage.GameSessionStateContext;
import teamnova.omok.glue.state.game.manage.GameSessionStateType;
import teamnova.omok.glue.state.game.state.CompletedGameSessionState;
import teamnova.omok.glue.state.game.state.LobbyGameSessionState;
import teamnova.omok.glue.state.game.state.MoveApplyingState;
import teamnova.omok.glue.state.game.state.MoveValidatingState;
import teamnova.omok.glue.state.game.state.OutcomeEvaluatingState;
import teamnova.omok.glue.state.game.state.PostGameDecisionResolvingState;
import teamnova.omok.glue.state.game.state.PostGameDecisionWaitingState;
import teamnova.omok.glue.state.game.state.SessionRematchPreparingState;
import teamnova.omok.glue.state.game.state.SessionTerminatingState;
import teamnova.omok.glue.state.game.state.TurnFinalizingState;
import teamnova.omok.glue.state.game.state.TurnWaitingState;
import teamnova.omok.glue.store.GameSession;
import teamnova.omok.modules.state_machine.StateMachineGateway;
import teamnova.omok.modules.state_machine.StateMachineGateway.Handle;
import teamnova.omok.modules.state_machine.interfaces.BaseEvent;
import teamnova.omok.modules.state_machine.interfaces.BaseState;
import teamnova.omok.modules.state_machine.interfaces.StateSignalListener;
import teamnova.omok.modules.state_machine.models.LifecycleEventKind;
import teamnova.omok.modules.state_machine.models.StateName;

public class GameStateHub {
    private static final Map<StateName, GameSessionStateType> STATE_NAME_LOOKUP =
        Arrays.stream(GameSessionStateType.values())
            .collect(Collectors.toUnmodifiableMap(GameSessionStateType::toStateName, type -> type));

    private final Handle stateMachine;
    private final GameSessionStateContext context;
    private GameSessionStateType currentType;

    public GameStateHub(GameSession session,
                        BoardService boardService,
                        TurnService turnService,
                        ScoreService scoreService) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(boardService, "boardService");
        Objects.requireNonNull(turnService, "turnService");

        this.context = new GameSessionStateContext(session, boardService, turnService, scoreService);
        this.stateMachine = StateMachineGateway.open();
        this.stateMachine.onTransition(this::handleTransition);
        // Register signal listener to trigger rules on TurnFinalizing enter only
        this.stateMachine.addStateSignalListener(new StateSignalListener() {
            private final Set<StateName> states = Set.of(GameSessionStateType.TURN_FINALIZING.toStateName());
            private final Set<LifecycleEventKind> events = Set.of(LifecycleEventKind.ON_START);
            @Override public Set<StateName> states() { return states; }
            @Override public Set<LifecycleEventKind> events() { return events; }
            @Override public void onSignal(StateName state, LifecycleEventKind kind) {
                triggerRules(STATE_NAME_LOOKUP.get(state));
            }
        });

        registerState(new LobbyGameSessionState());
        registerState(new TurnWaitingState());
        registerState(new MoveValidatingState());
        registerState(new MoveApplyingState());
        registerState(new OutcomeEvaluatingState());
        registerState(new TurnFinalizingState());
        registerState(new PostGameDecisionWaitingState());
        registerState(new PostGameDecisionResolvingState());
        registerState(new SessionRematchPreparingState());
        registerState(new SessionTerminatingState());
        registerState(new CompletedGameSessionState());

        this.stateMachine.start(GameSessionStateType.LOBBY.toStateName(), context);
    }

    private void registerState(BaseState state) {
        this.stateMachine.register(state);
    }

    private void handleTransition(StateName stateName) {
        GameSessionStateType resolved = STATE_NAME_LOOKUP.get(stateName);
        if (resolved == null) {
            throw new IllegalStateException("Unrecognised state: " + stateName.name());
        }
        if (resolved == currentType) {
            return;
        }
        currentType = resolved;
        // Do NOT trigger rules here to avoid duplicate calls; signals handle rule triggering
    }

    private void triggerRules(GameSessionStateType targetType) {
        if (targetType == null) return;
        RulesContext rulesContext = context.session().getRulesContext();
        if (rulesContext == null) {
            return;
        }
        rulesContext.attachStateContext(context);
        if (rulesContext.setCurrentRuleByGameState(targetType)) {
            rulesContext.activateCurrentRule();
        }
    }

    public GameSessionStateType currentType() {
        return currentType;
    }

    public GameSession session() {
        return context.session();
    }

    public GameSessionStateContext context() {
        return context;
    }

    public void submit(BaseEvent event, Consumer<GameSessionStateContext> callback) {
        Objects.requireNonNull(event, "event");
        stateMachine.submit(event, ctx -> {
            if (callback != null) {
                callback.accept((GameSessionStateContext) ctx);
            }
        });
    }

    public void process(long now) {
        stateMachine.process(context, now);
    }
}
