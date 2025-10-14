package teamnova.omok.domain.session.client.event;

public final class DisconnectClientEvent implements ClientEvent {
    @Override
    public ClientEventType type() {
        return ClientEventType.DISCONNECT;
    }
}
