package teamnova.omok.glue.service.dto;

/**
 * Movement validation outcome used during in-game turn processing.
 */
public enum MoveStatus {
    SUCCESS,
    INVALID_PLAYER,
    GAME_NOT_STARTED,
    OUT_OF_TURN,
    OUT_OF_BOUNDS,
    CELL_OCCUPIED,
    GAME_FINISHED
}
