package teamnova.omok.glue.state.client.manage;

import teamnova.omok.core.nio.ClientSession;

/**
 * Connection-level lifecycle phases for a {@link ClientSession}.
 */
public enum ClientStateType {
    CONNECTED,
    AUTHENTICATED,
    MATCHING,
    IN_GAME,
    DISCONNECTED
}
