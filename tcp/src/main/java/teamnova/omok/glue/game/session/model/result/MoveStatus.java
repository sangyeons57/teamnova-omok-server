package teamnova.omok.glue.game.session.model.result;

/**
 * Movement validation outcome used during in-game turn processing.
 */
public enum MoveStatus {
    SUCCESS("SUCCESS"),
    INVALID_PLAYER("INVALID_PLAYER"),
    GAME_NOT_STARTED("GAME_NOT_STARTED"),
    OUT_OF_TURN("OUT_OF_TURN"),
    OUT_OF_BOUNDS( "OUT_OF_BOUNDS"),
    CELL_OCCUPIED( "CELL_OCCUPIED"),
    GAME_FINISHED( "GAME_FINISHED"),
    RESTRICTED_ZONE( "RESTRICTED_ZONE");

    public final String name;
    MoveStatus(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
