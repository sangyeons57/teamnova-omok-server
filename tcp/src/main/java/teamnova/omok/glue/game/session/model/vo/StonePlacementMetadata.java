package teamnova.omok.glue.game.session.model.vo;

import java.util.Objects;

import teamnova.omok.glue.game.session.interfaces.GameTurnService;
import teamnova.omok.glue.game.session.model.dto.TurnSnapshot;

/**
 * Captures immutable metadata about a stone placement, including the turn information and origin.
 */
public record StonePlacementMetadata(int turnNumber,
                                     int placedByPlayerIndex,
                                     String placedByUserId,
                                     Source source) {
    public enum Source {
        PLAYER,
        RULE,
        SYSTEM
    }

    private static final StonePlacementMetadata EMPTY =
        new StonePlacementMetadata(0, -1, null, Source.SYSTEM);

    public StonePlacementMetadata {
        Objects.requireNonNull(source, "source");
        if (turnNumber < 0) {
            throw new IllegalArgumentException("turnNumber must be >= 0");
        }
        if (placedByPlayerIndex < -1) {
            throw new IllegalArgumentException("placedByPlayerIndex must be >= -1");
        }
    }

    public boolean isEmpty() {
        return turnNumber <= 0;
    }

    public static StonePlacementMetadata empty() {
        return EMPTY;
    }

    public static StonePlacementMetadata forPlayer(TurnSnapshot snapshot,
                                                   String userId,
                                                   int playerIndex) {
        Objects.requireNonNull(snapshot, "snapshot");
        return new StonePlacementMetadata(
            snapshot.turnNumber(),
            playerIndex,
            userId,
            Source.PLAYER
        );
    }

    public static StonePlacementMetadata forRule(TurnSnapshot snapshot,
                                                 int playerIndex,
                                                 String userId) {
        Objects.requireNonNull(snapshot, "snapshot");
        return new StonePlacementMetadata(
            snapshot.turnNumber(),
            playerIndex,
            userId,
            Source.RULE
        );
    }

    public static StonePlacementMetadata systemGenerated() {
        return EMPTY;
    }
}
