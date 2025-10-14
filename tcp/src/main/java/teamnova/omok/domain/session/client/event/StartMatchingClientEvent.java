package teamnova.omok.domain.session.client.event;

public final class StartMatchingClientEvent implements ClientEvent {
    @Override
    public ClientEventType type() {
        return ClientEventType.START_MATCHING;
    }
}
