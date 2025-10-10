package teamnova.omok.state.game.event;

public record DecisionTimeoutEvent(long triggerAt) implements GameSessionEvent {

    @Override
    public GameSessionEventType type() {
        return GameSessionEventType.DECISION_TIMEOUT;
    }
}
