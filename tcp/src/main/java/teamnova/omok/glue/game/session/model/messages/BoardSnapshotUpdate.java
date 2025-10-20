package teamnova.omok.glue.game.session.model.messages;

import java.util.Objects;

/**
 * Carries a full board snapshot that should be broadcast to participants.
 */
public final class BoardSnapshotUpdate {
    private final byte[] snapshot;
    private final long updatedAt;

    public BoardSnapshotUpdate(byte[] snapshot, long updatedAt) {
        this.snapshot = Objects.requireNonNull(snapshot, "snapshot").clone();
        this.updatedAt = updatedAt;
    }

    public byte[] snapshot() {
        return snapshot.clone();
    }

    public long updatedAt() {
        return updatedAt;
    }
}
