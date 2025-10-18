package teamnova.omok.glue.game.session.states;

import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;

import teamnova.omok.glue.game.session.interfaces.GameBoardService;
import teamnova.omok.glue.game.session.interfaces.GameScoreService;
import teamnova.omok.glue.game.session.interfaces.GameTurnService;
import teamnova.omok.glue.game.session.model.GameSession;
import teamnova.omok.glue.game.session.model.dto.GameSessionServices;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContext;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateType;
import teamnova.omok.glue.game.session.states.state.CompletedGameSessionState;
import teamnova.omok.glue.game.session.states.state.LobbyGameSessionState;
import teamnova.omok.glue.game.session.states.state.MoveApplyingState;
import teamnova.omok.glue.game.session.states.state.MoveValidatingState;
import teamnova.omok.glue.game.session.states.state.OutcomeEvaluatingState;
import teamnova.omok.glue.game.session.states.state.PostGameDecisionResolvingState;
import teamnova.omok.glue.game.session.states.state.PostGameDecisionWaitingState;
import teamnova.omok.glue.game.session.states.state.SessionRematchPreparingState;
import teamnova.omok.glue.game.session.states.state.SessionTerminatingState;
import teamnova.omok.glue.game.session.states.state.TurnFinalizingState;
import teamnova.omok.glue.game.session.states.state.TurnWaitingState;
import teamnova.omok.glue.rule.RulesContext;
import teamnova.omok.modules.state_machine.StateMachineGateway;
import teamnova.omok.modules.state_machine.StateMachineGateway.Handle;
import teamnova.omok.modules.state_machine.interfaces.BaseEvent;
import teamnova.omok.modules.state_machine.interfaces.BaseState;
import teamnova.omok.modules.state_machine.interfaces.StateSignalListener;
import teamnova.omok.modules.state_machine.models.LifecycleEventKind;
import teamnova.omok.modules.state_machine.models.StateName;

public class GameStateHub {

    private final Handle stateMachine;
    private final GameSessionStateContext context;
    private final GameSessionServices services;
    private GameSessionStateType currentType;

    public GameStateHub(GameSession session,
                        GameBoardService boardService,
                        GameTurnService turnService,
                        GameScoreService scoreService) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(boardService, "boardService");
        Objects.requireNonNull(turnService, "turnService");
        Objects.requireNonNull(scoreService, "scoreService");

        this.services = new GameSessionServices(boardService, turnService, scoreService);
        this.context = new GameSessionStateContext(session);
        this.stateMachine = StateMachineGateway.open();
        this.stateMachine.onTransition(this::handleTransition);
        // Register signal listener to trigger rules on TurnFinalizing enter only
        this.stateMachine.addStateSignalListener(new StateSignalListener() {
            private final Set<StateName> states = Set.of(GameSessionStateType.TURN_FINALIZING.toStateName());
            private final Set<LifecycleEventKind> events = Set.of(LifecycleEventKind.ON_START);
            @Override public Set<StateName> states() { return states; }
            @Override public Set<LifecycleEventKind> events() { return events; }
            @Override public void onSignal(StateName state, LifecycleEventKind kind) {
                triggerRules(GameSessionStateType.stateNameLookup(state));
            }
        });

        registerState(new LobbyGameSessionState(services.boardService(), services.turnService()));
        registerState(new TurnWaitingState(services.turnService()));
        registerState(new MoveValidatingState(services.boardService(), services.turnService()));
        registerState(new MoveApplyingState(services.boardService()));
        registerState(new OutcomeEvaluatingState(services.boardService(), services.scoreService()));
        registerState(new TurnFinalizingState(services.turnService()));
        registerState(new PostGameDecisionWaitingState(services.turnService()));
        registerState(new PostGameDecisionResolvingState());
        registerState(new SessionRematchPreparingState());
        registerState(new SessionTerminatingState());
        registerState(new CompletedGameSessionState(services.turnService()));

        this.stateMachine.start(GameSessionStateType.LOBBY.toStateName(), context);
    }

    private void registerState(BaseState state) {
        this.stateMachine.register(state);
    }

    private void handleTransition(StateName stateName) {
        GameSessionStateType resolved = GameSessionStateType.stateNameLookup(stateName);
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
        rulesContext.attachServices(services);
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
