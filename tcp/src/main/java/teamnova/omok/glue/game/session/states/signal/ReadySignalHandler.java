package teamnova.omok.glue.game.session.states.signal;

import java.util.Set;

import teamnova.omok.glue.game.session.model.result.ReadyResult;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContext;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContextService;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateType;
import teamnova.omok.modules.state_machine.interfaces.StateSignalListener;
import teamnova.omok.modules.state_machine.models.LifecycleEventKind;
import teamnova.omok.modules.state_machine.models.StateName;

/**
 * Handles READY flow via state-based signals.
     * - LOBBY ON_UPDATE/ON_TRANSITION: drain ReadyResult and respond/broadcast accordingly.
     * - GAME_SESSION_STARTED is broadcast exactly once here when all players become ready before leaving the lobby.
 */
public final class ReadySignalHandler implements StateSignalListener {
    private final GameSessionStateContext context;
    private final GameSessionStateContextService contextService;
    private final teamnova.omok.glue.game.session.model.dto.GameSessionServices services;

    public ReadySignalHandler(GameSessionStateContext context,
                              GameSessionStateContextService contextService,
                              teamnova.omok.glue.game.session.model.dto.GameSessionServices services) {
        this.context = context;
        this.contextService = contextService;
        this.services = services;
    }

    @Override
    public Set<LifecycleEventKind> events() {
        return java.util.Set.of(
            LifecycleEventKind.ON_UPDATE,
            LifecycleEventKind.ON_TRANSITION
        );
    }

    @Override
    public Set<StateName> states() {
        return java.util.Set.of(
            GameSessionStateType.LOBBY.toStateName()
        );
    }

    @Override
    public void onSignal(StateName state, LifecycleEventKind kind) {
        GameSessionStateType type = GameSessionStateType.stateNameLookup(state);
        if (type == GameSessionStateType.LOBBY &&
            (kind == LifecycleEventKind.ON_UPDATE || kind == LifecycleEventKind.ON_TRANSITION)) {
            handleLobbyUpdate();
        }
    }

    private void handleLobbyUpdate() {
        ReadyResult result = contextService.turn().consumeReadyResult(context);
        if (result == null) {
            return;
        }
        // Respond to the requester
        services.messenger().respondReady(result.userId(), result.requestId(), context.session(), result);
        // Broadcast state change
        if (result.stateChanged()) {
            services.messenger().broadcastReady(context.session(), result);
        }
        // If game starts now, broadcast GAME_SESSION_STARTED once here (leaving Lobby)
        if (result.gameStartedNow()) {
            services.messenger().broadcastGameStart(context.session(), result.firstTurn());
        }
    }
}
