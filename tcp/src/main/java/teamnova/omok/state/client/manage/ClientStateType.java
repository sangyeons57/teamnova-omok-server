package teamnova.omok.state.client.manage;

/**
 * Connection-level lifecycle phases for a {@link teamnova.omok.nio.ClientSession}.
 */
public enum ClientStateType {
    CONNECTED,
    AUTHENTICATED,
    MATCHING,
    IN_GAME,
    DISCONNECTED
}
