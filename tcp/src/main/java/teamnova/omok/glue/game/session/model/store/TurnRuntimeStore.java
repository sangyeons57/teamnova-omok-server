package teamnova.omok.glue.game.session.model.store;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;

import teamnova.omok.glue.game.session.model.dto.TurnSnapshot;
import teamnova.omok.glue.game.session.model.runtime.TurnPersonalFrame;

/**
 * Maintains per-overall-turn runtime data and personal turn frames.
 */
public final class TurnRuntimeStore {

    private final List<TurnPersonalFrame> personalTurns = new ArrayList<>();
    private final Deque<TurnSnapshot> pendingSnapshots = new ArrayDeque<>();

    private TurnPersonalFrame activeFrame;
    private TurnPersonalFrame pendingOutcomeFrame;
    private TurnPersonalFrame pendingTimeoutFrame;

    public void enqueueSnapshot(TurnSnapshot snapshot, long timestampMillis) {
        if (snapshot == null) {
            return;
        }
        pendingSnapshots.addLast(snapshot);
    }

    public TurnSnapshot peekSnapshot() {
        return pendingSnapshots.peekFirst();
    }

    public TurnSnapshot pollSnapshot() {
        return pendingSnapshots.pollFirst();
    }

    public void resetPersonalTurnFrames() {
        personalTurns.clear();
        activeFrame = null;
        pendingOutcomeFrame = null;
        pendingTimeoutFrame = null;
        pendingSnapshots.clear();
    }

    public TurnPersonalFrame startPersonalTurnFrame(TurnSnapshot snapshot,
                                                    long startedAt) {
        TurnPersonalFrame frame = TurnPersonalFrame.fromSnapshot(snapshot, startedAt);
        personalTurns.add(frame);
        activeFrame = frame;
        return frame;
    }

    public TurnPersonalFrame activePersonalTurnFrame() {
        return activeFrame;
    }

    public void recordMoveOutcome(TurnPersonalFrame frame) {
        if (frame == null) {
            throw new IllegalArgumentException("frame");
        }
        pendingOutcomeFrame = frame;
    }

    public TurnPersonalFrame consumeMoveOutcome() {
        TurnPersonalFrame frame = pendingOutcomeFrame;
        pendingOutcomeFrame = null;
        if (frame != null) {
            frame.markOutcomeConsumed();
        }
        return frame;
    }

    public void recordTimeoutOutcome(TurnPersonalFrame frame) {
        pendingTimeoutFrame = frame;
    }

    public TurnPersonalFrame consumeTimeoutOutcome() {
        TurnPersonalFrame frame = pendingTimeoutFrame;
        pendingTimeoutFrame = null;
        return frame;
    }

    public List<TurnPersonalFrame> personalTurnFrames() {
        return Collections.unmodifiableList(personalTurns);
    }
}
