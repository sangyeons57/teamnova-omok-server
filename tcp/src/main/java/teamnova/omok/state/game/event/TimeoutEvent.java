package teamnova.omok.state.game.event;

public record TimeoutEvent(int expectedTurnNumber, long timestamp)
    implements GameSessionEvent {

    @Override
    public GameSessionEventType type() {
        return GameSessionEventType.TIMEOUT;
    }
}
