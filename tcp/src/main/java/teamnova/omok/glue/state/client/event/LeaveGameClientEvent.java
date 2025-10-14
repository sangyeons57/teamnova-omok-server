package teamnova.omok.glue.state.client.event;

public final class LeaveGameClientEvent implements ClientEvent {
    @Override
    public ClientEventType type() {
        return ClientEventType.LEAVE_GAME;
    }
}
