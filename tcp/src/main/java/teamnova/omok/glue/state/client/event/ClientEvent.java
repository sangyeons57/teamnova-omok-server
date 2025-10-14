package teamnova.omok.glue.state.client.event;

import teamnova.omok.glue.state.client.manage.ClientStateManager;

/**
 * Marker for events routed through {@link ClientStateManager}.
 */
public interface ClientEvent {
    ClientEventType type();
}
