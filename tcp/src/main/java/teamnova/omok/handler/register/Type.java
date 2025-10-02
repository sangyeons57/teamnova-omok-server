package teamnova.omok.handler.register;

public enum Type {
    HELLO(0),
    AUTH(1),
    PINGPONG(2),
    JOIN_MATCH(3),
    JOIN_IN_GAME_SESSION(4),
    LEAVE_IN_GAME_SESSION(5),
    ;
    public final int value;

    Type(int value){
        this.value = value;
    }
}
