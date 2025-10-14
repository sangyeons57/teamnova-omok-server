package teamnova.omok.glue.state.game;

import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import teamnova.omok.glue.rule.RulesContext;
import teamnova.omok.glue.service.BoardService;
import teamnova.omok.glue.service.OutcomeService;
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
import teamnova.omok.modules.state_machine.StateMachine;
import teamnova.omok.modules.state_machine.interfaces.BaseEvent;
import teamnova.omok.modules.state_machine.interfaces.BaseState;
import teamnova.omok.modules.state_machine.interfaces.StateMachineManager;
import teamnova.omok.modules.state_machine.models.StateName;

public class GameStateHub {
    private static final Map<StateName, GameSessionStateType> STATE_NAME_LOOKUP =
        Arrays.stream(GameSessionStateType.values())
            .collect(Collectors.toUnmodifiableMap(GameSessionStateType::toStateName, type -> type));

    private final StateMachineManager stateMachineManager;
    private final GameSessionStateContext context;
    private GameSessionStateType currentType;

    public GameStateHub(GameSession session,
                        BoardService boardService,
                        TurnService turnService,
                        OutcomeService outcomeService) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(boardService, "boardService");
        Objects.requireNonNull(turnService, "turnService");
        Objects.requireNonNull(outcomeService, "outcomeService");

        this.context = new GameSessionStateContext(session, boardService, turnService, outcomeService);
        this.stateMachineManager = StateMachine.createStateMatchingManager();
        this.stateMachineManager.onTransition(this::handleTransition);

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

        this.stateMachineManager.start(GameSessionStateType.LOBBY.toStateName(), context);
    }

    private void registerState(BaseState state) {
        this.stateMachineManager.registerState(state);
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
        triggerRules(resolved);
    }

    private void triggerRules(GameSessionStateType targetType) {
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
        stateMachineManager.submit(event, ctx -> {
            if (callback != null) {
                callback.accept((GameSessionStateContext) ctx);
            }
        });
    }

    public void process(long now) {
        stateMachineManager.process(context, now);
    }
}
