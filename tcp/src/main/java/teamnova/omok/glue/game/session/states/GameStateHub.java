package teamnova.omok.glue.game.session.states;

import java.util.Objects;

import teamnova.omok.glue.game.session.interfaces.*;
import teamnova.omok.glue.game.session.interfaces.manager.TurnTimeoutScheduler;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionAccess;
import teamnova.omok.glue.game.session.log.GameSessionLogger;
import teamnova.omok.glue.game.session.model.GameSession;
import teamnova.omok.glue.game.session.model.dto.GameSessionServices;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContext;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContextService;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateType;
import teamnova.omok.glue.game.session.states.signal.*;
import teamnova.omok.glue.game.session.states.state.CompletedGameSessionState;
import teamnova.omok.glue.game.session.states.state.LobbyGameSessionState;
import teamnova.omok.glue.game.session.states.state.MoveApplyingState;
import teamnova.omok.glue.game.session.states.state.MoveValidatingState;
import teamnova.omok.glue.game.session.states.state.PostGameDecisionResolvingState;
import teamnova.omok.glue.game.session.states.state.PostGameDecisionWaitingState;
import teamnova.omok.glue.game.session.states.state.SessionRematchPreparingState;
import teamnova.omok.glue.game.session.states.state.SessionTerminatingState;
import teamnova.omok.glue.game.session.states.state.TurnPersonalEndState;
import teamnova.omok.glue.game.session.states.state.TurnPersonalStartState;
import teamnova.omok.glue.game.session.states.state.TurnEndState;
import teamnova.omok.glue.game.session.states.state.TurnStartState;
import teamnova.omok.glue.game.session.states.state.TurnWaitingState;
import teamnova.omok.modules.state_machine.StateMachineGateway;
import teamnova.omok.modules.state_machine.StateMachineGateway.Handle;
import teamnova.omok.modules.state_machine.interfaces.BaseEvent;
import teamnova.omok.modules.state_machine.interfaces.BaseState;
import teamnova.omok.modules.state_machine.interfaces.StateSignalListener;

public class GameStateHub {

    private final Handle stateMachine;
    private final GameSessionStateContext context;
    private final GameSessionServices services;
    private final GameSessionStateContextService contextService;
    private GameSessionStateType currentType;

    public GameStateHub(GameSession session,
                        GameBoardService boardService,
                        GameTurnService turnService,
                        GameScoreService scoreService,
                        GameSessionStateContextService contextService,
                        GameSessionMessenger messenger,
                        TurnTimeoutScheduler turnTimeoutScheduler,
                        DecisionTimeoutScheduler decisionTimeoutScheduler,
                        GameSessionRepository repository,
                        GameSessionRuntime runtime) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(boardService, "boardService");
        Objects.requireNonNull(turnService, "turnService");
        Objects.requireNonNull(scoreService, "scoreService");
        Objects.requireNonNull(contextService, "contextService");
        Objects.requireNonNull(messenger, "messenger");

        this.contextService = contextService;
        this.services = new GameSessionServices(boardService, turnService, scoreService, messenger, turnTimeoutScheduler, decisionTimeoutScheduler, repository, runtime);
        this.context = new GameSessionStateContext(session);

        this.stateMachine = StateMachineGateway.open();
        // Register lifecycle logging via dedicated handler file
        registerStateConfig(contextService);
        registerSignalHandler();

        this.stateMachine.start(GameSessionStateType.LOBBY.toStateName(), context);
    }

    private void registerStateConfig(GameSessionStateContextService contextService) {
        registerState(new LobbyGameSessionState(contextService, services));
        registerState(new TurnStartState(contextService));
        registerState(new TurnPersonalStartState(contextService, services));
        registerState(new TurnWaitingState(contextService, services.turnService(), services.messenger()));
        registerState(new MoveValidatingState(contextService, services.boardService(), services.turnService()));
        registerState(new MoveApplyingState(contextService, services));
        registerState(new TurnPersonalEndState(contextService, services));
        registerState(new TurnEndState(contextService, services));
        registerState(new PostGameDecisionWaitingState(contextService, services.turnService()));
        registerState(new PostGameDecisionResolvingState(contextService));
        registerState(new SessionRematchPreparingState(contextService));
        registerState(new SessionTerminatingState(contextService));
        registerState(new CompletedGameSessionState(contextService, services.turnService()));
    }

    private void registerSignalHandler() {
        // Register state-based signal handlers (outbound I/O)
        registerSignalListener(new LifecycleLoggingSignalHandler(this, context));
        registerSignalListener(new ReadySignalHandler(context, this.contextService, services));
        registerSignalListener(new MoveSignalHandler(context, services));
        registerSignalListener(new PostGameSignalHandler(this, context, this.contextService, services));
        registerSignalListener(new TimeoutSignalHandler(context, services));
    }

    private void registerState(BaseState state) {
        this.stateMachine.register(state);
    }

    private void registerSignalListener(StateSignalListener listener) { this.stateMachine.addStateSignalListener(listener);}


    public GameSessionStateType currentType() {
        return currentType;
    }

    // Updater for lifecycle logging handler
    public void updateCurrentType(GameSessionStateType next) {
        this.currentType = next;
    }

    public GameSessionAccess session() {
        return context.session();
    }


    public void submit(BaseEvent event) {
        Objects.requireNonNull(event, "event");
        stateMachine.submit(event);
    }

    public void process(long now) {
        stateMachine.process(context, now);
    }
}
