package teamnova.omok.domain.session.client.event;

public final class AuthenticatedClientEvent implements ClientEvent {
    @Override
    public ClientEventType type() {
        return ClientEventType.AUTHENTICATED;
    }
}
