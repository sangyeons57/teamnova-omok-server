package teamnova.omok.glue.state.client.event;

public final class AuthenticatedClientEvent implements ClientEvent {
    @Override
    public ClientEventType type() {
        return ClientEventType.AUTHENTICATED;
    }
}
