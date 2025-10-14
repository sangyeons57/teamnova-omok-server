package teamnova.omok.domain.session.client.event;

public final class CancelMatchingClientEvent implements ClientEvent {
    @Override
    public ClientEventType type() {
        return ClientEventType.CANCEL_MATCHING;
    }
}
