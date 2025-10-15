package teamnova.omok.glue.handler.register;

public enum Type {
    HELLO(0),
    AUTH(1),
    PINGPONG(2),
    JOIN_MATCH(3),
    JOIN_IN_GAME_SESSION(4),
    LEAVE_IN_GAME_SESSION(5),
    READY_IN_GAME_SESSION(6),
    GAME_SESSION_STARTED(7),
    PLACE_STONE(8),
    STONE_PLACED(9),
    TURN_TIMEOUT(10),
    GAME_SESSION_COMPLETED(11),
    GAME_POST_DECISION_PROMPT(12),
    POST_GAME_DECISION(13),
    GAME_POST_DECISION_UPDATE(14),
    GAME_SESSION_REMATCH_STARTED(15),
    GAME_SESSION_TERMINATED(16),
    GAME_SESSION_PLAYER_DISCONNECTED(17),
    BOARD_SNAPSHOT(18),
    LEAVE_MATCH(19),

    ERROR(255),
    ;
    public final byte value;

    Type(int value){
        this.value = (byte)value;
    }

    public static Type lookup(byte value) {
        for (Type type : values()) {
            if (type.value == value) {
                return type;
            }
        }
        return ERROR;
    }

    public static Type lookup(int value) {
        return lookup(value & 0xFF);
    }
}
