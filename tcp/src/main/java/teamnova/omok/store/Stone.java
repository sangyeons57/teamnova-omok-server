package teamnova.omok.store;

/**
 * Represents a single stone on the Omok board, encoded as a byte for compact storage.
 */
public enum Stone {
    EMPTY((byte) 0),
    PLAYER1((byte) 1),
    PLAYER2((byte) 2),
    PLAYER3((byte) 3),
    PLAYER4((byte) 4);

    private final byte code;

    Stone(byte code) {
        this.code = code;
    }

    public byte code() {
        return code;
    }

    public static Stone fromByte(byte code) {
        return switch (code) {
            case 1 -> PLAYER1;
            case 2 -> PLAYER2;
            case 3 -> PLAYER3;
            case 4 -> PLAYER4;
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
}
