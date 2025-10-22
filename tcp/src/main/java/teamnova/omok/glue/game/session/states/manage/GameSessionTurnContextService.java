package teamnova.omok.glue.game.session.states.manage;

import java.util.Objects;

import teamnova.omok.glue.game.session.model.dto.TurnSnapshot;
import teamnova.omok.glue.game.session.model.result.ReadyResult;
import teamnova.omok.glue.game.session.model.runtime.TurnPersonalFrame;
import teamnova.omok.glue.game.session.model.result.MoveStatus;

/**
 * Stateless operations for managing turn-related buffers within {@link GameSessionStateContext}.
 */
public final class GameSessionTurnContextService {

    public void resetPersonalTurns(GameSessionStateContext context) {
        Objects.requireNonNull(context, "context");
        context.turnRuntime().resetPersonalTurnFrames();
    }

    public void beginPersonalTurn(GameSessionStateContext context,
                                  TurnSnapshot snapshot,
                                  long startedAt) {
        Objects.requireNonNull(context, "context");
        context.turnRuntime().beginPersonalTurnFrame(snapshot, startedAt);
    }

    public TurnPersonalFrame currentPersonalTurn(GameSessionStateContext context) {
        Objects.requireNonNull(context, "context");
        return context.turnRuntime().currentPersonalTurnFrame();
    }

    public boolean hasActiveTurnCycle(GameSessionStateContext context) {
        Objects.requireNonNull(context, "context");
        TurnPersonalFrame frame = context.turnRuntime().currentPersonalTurnFrame();
        return frame != null && frame.hasActiveMove();
    }

    public void beginTurnCycle(GameSessionStateContext context,
                               String userId,
                               int x,
                               int y,
                               long requestedAtMillis,
                               long requestId) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(userId, "userId");
        TurnPersonalFrame frame = context.turnRuntime().currentPersonalTurnFrame();
        if (frame == null) {
            throw new IllegalStateException("No active personal turn frame available");
        }
        if (frame.hasActiveMove()) {
            throw new IllegalStateException("Turn cycle already in progress");
        }
        frame.beginMove(userId, x, y, requestedAtMillis, requestId);
    }

    public void clearTurnCycle(GameSessionStateContext context) {
        Objects.requireNonNull(context, "context");
        TurnPersonalFrame frame = context.turnRuntime().currentPersonalTurnFrame();
        if (frame != null) {
            frame.endActiveMove();
        }
    }

    public void finalizeMoveOutcome(GameSessionStateContext context, MoveStatus status) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(status, "status");
        TurnPersonalFrame frame = context.turnRuntime().currentPersonalTurnFrame();
        if (frame == null) {
            throw new IllegalStateException("No active personal turn frame available");
        }
        frame.resolveOutcome(status);
    }

    public void queueReadyResult(GameSessionStateContext context, ReadyResult result) {
        Objects.requireNonNull(context, "context");
        context.turnRuntime().setPendingReadyResult(result);
    }

    public ReadyResult consumeReadyResult(GameSessionStateContext context) {
        Objects.requireNonNull(context, "context");
        ReadyResult result = context.turnRuntime().getPendingReadyResult();
        context.turnRuntime().clearPendingReadyResult();
        return result;
    }

    public void recordTimeoutOutcome(GameSessionStateContext context,
                                     boolean timedOut,
                                     TurnSnapshot snapshot,
                                     long occurredAtMillis) {
        Objects.requireNonNull(context, "context");
        TurnPersonalFrame frame = context.turnRuntime().currentPersonalTurnFrame();
        if (frame == null) {
            return;
        }
        frame.resolveTimeout(timedOut, snapshot, occurredAtMillis);
        context.turnRuntime().setPendingTimeoutFrame(frame);
    }

    public TurnPersonalFrame consumeTimeoutOutcome(GameSessionStateContext context) {
        Objects.requireNonNull(context, "context");
        return context.turnRuntime().consumePendingTimeoutFrame();
    }

    public void recordTurnSnapshot(GameSessionStateContext context,
                                   TurnSnapshot snapshot,
                                   long occurredAtMillis) {
        Objects.requireNonNull(context, "context");
        context.turnRuntime().setPendingTurnSnapshot(snapshot, occurredAtMillis);
    }

    public TurnSnapshot peekTurnSnapshot(GameSessionStateContext context) {
        Objects.requireNonNull(context, "context");
        return context.turnRuntime().getPendingTurnSnapshot();
    }

    public TurnSnapshot consumeTurnSnapshot(GameSessionStateContext context) {
        Objects.requireNonNull(context, "context");
        TurnSnapshot snapshot = context.turnRuntime().getPendingTurnSnapshot();
        context.turnRuntime().clearPendingTurnSnapshot();
        return snapshot;
    }
}
