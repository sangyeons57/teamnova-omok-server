package teamnova.omok.glue.service.dto;

import java.util.Objects;

import teamnova.omok.glue.store.GameSession;

/**
 * Carries a full board snapshot that should be broadcast to participants.
 */
public final class BoardSnapshotUpdate {
    private final GameSession session;
    private final byte[] snapshot;
    private final long updatedAt;

    public BoardSnapshotUpdate(GameSession session, byte[] snapshot, long updatedAt) {
        this.session = Objects.requireNonNull(session, "session");
        this.snapshot = Objects.requireNonNull(snapshot, "snapshot").clone();
        this.updatedAt = updatedAt;
    }

    public GameSession session() {
        return session;
    }

    public byte[] snapshot() {
        return snapshot.clone();
    }

    public long updatedAt() {
        return updatedAt;
    }
}
