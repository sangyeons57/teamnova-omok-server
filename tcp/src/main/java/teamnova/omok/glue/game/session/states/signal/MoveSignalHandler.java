package teamnova.omok.glue.game.session.states.signal;

import java.util.Set;

import teamnova.omok.glue.game.session.model.dto.TurnSnapshot;
import teamnova.omok.glue.game.session.model.runtime.TurnPersonalFrame;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContext;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateType;
import teamnova.omok.modules.state_machine.interfaces.StateSignalListener;
import teamnova.omok.modules.state_machine.models.LifecycleEventKind;
import teamnova.omok.modules.state_machine.models.StateName;

/**
 * State-based signal handler for move/turn related side effects.
 * - Broadcasts TURN_STARTED when entering TURN_PERSONAL_START.
 *
 * Outbound I/O is centralized here instead of state classes to improve separation of concerns.
 */
public final class MoveSignalHandler implements StateSignalListener {
    private final GameSessionStateContext context;
    private final teamnova.omok.glue.game.session.model.dto.GameSessionServices services;

    public MoveSignalHandler(GameSessionStateContext context,
                             teamnova.omok.glue.game.session.model.dto.GameSessionServices services) {
        this.context = context;
        this.services = services;
    }

    @Override
    public Set<LifecycleEventKind> events() {
        return java.util.Set.of(LifecycleEventKind.ON_START);
    }

    @Override
    public Set<StateName> states() {
        return java.util.Set.of(
            GameSessionStateType.MOVE_APPLYING.toStateName(),
            GameSessionStateType.TURN_PERSONAL_START.toStateName()
        );
    }

    @Override
    public void onSignal(StateName state, LifecycleEventKind kind) {
        if (kind != LifecycleEventKind.ON_START) return;
        GameSessionStateType type = GameSessionStateType.stateNameLookup(state);
        if (type == GameSessionStateType.MOVE_APPLYING) {
            // Send ACK to the requester as soon as placement is applied to the board
            TurnPersonalFrame frame = context.turnRuntime().currentPersonalTurnFrame();
            if (frame != null) {
                services.messenger().respondMove(frame.userId(), frame.stonePlaceRequestId(), context.session(), frame);
            }
            // Broadcast board update after stone placement is applied
            byte[] boardSnapshot = services.boardService().snapshot(context.board());
            teamnova.omok.glue.game.session.model.messages.BoardSnapshotUpdate update =
                new teamnova.omok.glue.game.session.model.messages.BoardSnapshotUpdate(
                    boardSnapshot,
                    System.currentTimeMillis()
                );
            services.messenger().broadcastBoardSnapshot(context.session(), update);
            return;
        }
        if (type == GameSessionStateType.TURN_PERSONAL_START) {
            TurnPersonalFrame frame = context.turnRuntime().currentPersonalTurnFrame();
            TurnSnapshot snapshot = null;
            if (frame != null) {
                snapshot = frame.currentSnapshot();
            }
            services.messenger().broadcastTurnStarted(context.session(), snapshot);
            if (snapshot != null) {
                services.turnTimeoutScheduler().schedule(context.session(), snapshot,
                    teamnova.omok.glue.game.session.GameSessionManager.getInstance());
            }
        }
    }
}
