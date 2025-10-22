package teamnova.omok.glue.game.session.states;

import java.util.Objects;
import java.util.function.Consumer;

import teamnova.omok.glue.game.session.interfaces.GameBoardService;
import teamnova.omok.glue.game.session.interfaces.GameScoreService;
import teamnova.omok.glue.game.session.interfaces.GameTurnService;
import teamnova.omok.glue.game.session.interfaces.GameSessionMessenger;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionAccess;
import teamnova.omok.glue.game.session.log.GameSessionLogger;
import teamnova.omok.glue.game.session.model.GameSession;
import teamnova.omok.glue.game.session.model.dto.GameSessionServices;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContext;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContextService;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateType;
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
                        GameScoreService scoreService,
                        GameSessionStateContextService contextService,
                        GameSessionMessenger messenger) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(boardService, "boardService");
        Objects.requireNonNull(turnService, "turnService");
        Objects.requireNonNull(scoreService, "scoreService");
        Objects.requireNonNull(contextService, "contextService");
        Objects.requireNonNull(messenger, "messenger");

        this.services = new GameSessionServices(boardService, turnService, scoreService, messenger);
        this.context = new GameSessionStateContext(session);

        this.stateMachine = StateMachineGateway.open();
        this.stateMachine.addStateSignalListener(new StateSignalListener() {
            @Override
            public java.util.Set<LifecycleEventKind> events() { return java.util.Set.of(LifecycleEventKind.ON_EXIT, LifecycleEventKind.ON_START); }
            @Override
            public void onSignal(StateName state, LifecycleEventKind kind) {
                if (kind == LifecycleEventKind.ON_EXIT) {
                    GameSessionStateType type = GameSessionStateType.stateNameLookup(state);
                    if (type != null) {
                        GameSessionLogger.exit(context, type);
                    }
                } else if (kind == LifecycleEventKind.ON_START) {
                    GameSessionStateType to = GameSessionStateType.stateNameLookup(state);
                    if (to == null) {
                        throw new IllegalStateException("Unrecognised state: " + state.name());
                    }
                    GameSessionStateType from = currentType;
                    if (to != from) {
                        GameSessionLogger.transition(context, from, to, "state-machine");
                        GameSessionLogger.enter(context, to);
                        currentType = to;
                    }
                }
            }
        });
        registerStateConfig(contextService);

        this.stateMachine.start(GameSessionStateType.LOBBY.toStateName(), context);
    }

    private void registerStateConfig(GameSessionStateContextService contextService) {
        registerState(new LobbyGameSessionState(contextService, services));
        registerState(new TurnStartState(contextService));
        registerState(new TurnPersonalStartState(contextService, services));
        registerState(new TurnWaitingState(contextService, services.turnService()));
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

    private void registerState(BaseState state) {
        this.stateMachine.register(state);
    }



    public GameSessionStateType currentType() {
        return currentType;
    }

    public GameSessionAccess session() {
        return context.session();
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
