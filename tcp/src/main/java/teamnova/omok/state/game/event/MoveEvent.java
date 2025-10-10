package teamnova.omok.state.game.event;

public record MoveEvent(String userId, int x, int y, long timestamp, long requestId)
    implements GameSessionEvent {

    @Override
    public GameSessionEventType type() {
        return GameSessionEventType.MOVE;
    }
}
