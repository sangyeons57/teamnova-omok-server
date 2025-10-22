package teamnova.omok.glue.game.session.states.signal;

import java.util.Set;

import teamnova.omok.glue.game.session.model.dto.TurnSnapshot;
import teamnova.omok.glue.game.session.model.result.ReadyResult;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContext;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContextService;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateType;
import teamnova.omok.modules.state_machine.interfaces.StateSignalListener;
import teamnova.omok.modules.state_machine.models.LifecycleEventKind;
import teamnova.omok.modules.state_machine.models.StateName;

/**
 * Handles READY flow via state-based signals.
 * - LOBBY ON_UPDATE: drain ReadyResult and respond/broadcast accordingly.
 * - TURN_START ON_START: broadcast game-start and schedule first turn timeout.
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
        return java.util.Set.of(LifecycleEventKind.ON_UPDATE, LifecycleEventKind.ON_START);
    }

    @Override
    public Set<StateName> states() {
        return java.util.Set.of(
            GameSessionStateType.LOBBY.toStateName(),
            GameSessionStateType.TURN_START.toStateName()
        );
    }

    @Override
    public void onSignal(StateName state, LifecycleEventKind kind) {
        GameSessionStateType type = GameSessionStateType.stateNameLookup(state);
        if (type == GameSessionStateType.LOBBY && kind == LifecycleEventKind.ON_UPDATE) {
            handleLobbyUpdate();
        } else if (type == GameSessionStateType.TURN_START && kind == LifecycleEventKind.ON_START) {
            handleTurnStart();
        }
    }

    private void handleLobbyUpdate() {
        ReadyResult result = contextService.turn().consumeReadyResult(context);
        if (result == null) {
            return;
        }
        // Respond to the requester
        services.messenger().respondReady(result.userId(), 0L, context.session(), result);
        // Broadcast state change
        if (result.stateChanged()) {
            services.messenger().broadcastReady(context.session(), result);
        }
        // If game started now, game start broadcast will run on TURN_START ON_START
    }

    private void handleTurnStart() {
        TurnSnapshot turn = contextService.turn().peekTurnSnapshot(context);
        if (turn == null) {
            turn = services.turnService().snapshot(context.turns());
        }
        services.messenger().broadcastGameStart(context.session(), turn);
        if (turn != null) {
            services.turnTimeoutScheduler().schedule(context.session(), turn,
                teamnova.omok.glue.game.session.GameSessionManager.getInstance());
        }
    }
}
