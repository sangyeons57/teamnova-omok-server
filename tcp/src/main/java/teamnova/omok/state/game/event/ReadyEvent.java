package teamnova.omok.state.game.event;

public record ReadyEvent(String userId, long timestamp, long requestId)
    implements GameSessionEvent {

    @Override
    public GameSessionEventType type() {
        return GameSessionEventType.READY;
    }
}
