package teamnova.omok.glue.state.client.event;

public final class CancelMatchingClientEvent implements ClientEvent {
    @Override
    public ClientEventType type() {
        return ClientEventType.CANCEL_MATCHING;
    }
}
