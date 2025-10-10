package teamnova.omok.state.client.event;

public final class AuthenticatedClientEvent implements ClientEvent {
    @Override
    public ClientEventType type() {
        return ClientEventType.AUTHENTICATED;
    }
}
