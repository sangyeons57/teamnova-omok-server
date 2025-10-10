package teamnova.omok.state.client.event;

/**
 * Event types that can be emitted for {@code ClientStateManager}.
 */
public enum ClientEventType {
    AUTHENTICATED,
    START_MATCHING,
    CANCEL_MATCHING,
    ENTER_GAME,
    LEAVE_GAME,
    DISCONNECT
}
