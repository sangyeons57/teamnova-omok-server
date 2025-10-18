package teamnova.omok.glue.game.session.model;

/**
 * Represents a single stone on the Omok board, encoded as a byte for compact storage.
 */
public enum Stone {
    EMPTY((byte) -1),
    PLAYER1((byte) 0),
    PLAYER2((byte) 1),
    PLAYER3((byte) 2),
    PLAYER4((byte) 3),
    JOKER((byte) 4),
    BLOCKER((byte) 5);

    private final byte code;

    Stone(byte code) {
        this.code = code;
    }

    public byte code() {
        return code;
    }

    public static Stone fromByte(byte code) {
        return switch (code) {
            case 0 -> PLAYER1;
            case 1 -> PLAYER2;
            case 2 -> PLAYER3;
            case 3 -> PLAYER4;
            case 4 -> JOKER;
            case 5 -> BLOCKER;
            default -> EMPTY;
        };
    }

    public static Stone fromPlayerOrder(int order) {
        return switch (order) {
            case 0 -> PLAYER1;
            case 1 -> PLAYER2;
            case 2 -> PLAYER3;
            case 3 -> PLAYER4;
            default -> EMPTY;
        };
    }

    public boolean isPlayerStone() {
        return switch (this) {
            case PLAYER1, PLAYER2, PLAYER3, PLAYER4 -> true;
            default -> false;
        };
    }

    public boolean isWildcard() {
        return this == JOKER;
    }

    public boolean isBlocking() {
        return this == BLOCKER;
    }

    /**
     * Returns true if this stone should count towards a five-in-a-row sequence for the given player's stone.
     */
    public boolean countsForPlayerSequence(Stone playerStone) {
        if (playerStone == null || !playerStone.isPlayerStone()) {
            return false;
        }
        if (this == EMPTY || this.isBlocking()) {
            return false;
        }
        if (this.isWildcard()) {
            return true;
        }
        return this == playerStone;
    }
}
