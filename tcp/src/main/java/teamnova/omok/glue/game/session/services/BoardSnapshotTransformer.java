package teamnova.omok.glue.game.session.services;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionAccess;

/**
 * Applies view-time transformations to raw board snapshots before they are
 * broadcast to clients. Implementations may inspect the session and
 * conditionally rewrite the byte array while keeping the core board service
 * free of rule-specific logic.
 */
@FunctionalInterface
public interface BoardSnapshotTransformer {
    BoardSnapshotTransformer IDENTITY = (session, snapshot) -> snapshot;

    byte[] transform(GameSessionAccess session, byte[] snapshot);
}
