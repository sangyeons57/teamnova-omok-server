package teamnova.omok.handler.register;

import java.nio.ByteBuffer;

public enum Type {
    HELLO(0),
    AUTH(1),
    PINGPONG(2),
    JOIN_MATCH(3),
    JOIN_IN_GAME_SESSION(4),
    LEAVE_IN_GAME_SESSION(5),
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
