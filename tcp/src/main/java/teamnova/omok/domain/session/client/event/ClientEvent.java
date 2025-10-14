package teamnova.omok.domain.session.client.event;

import teamnova.omok.domain.session.client.manage.ClientStateManager;

/**
 * Marker for events routed through {@link ClientStateManager}.
 */
public interface ClientEvent {
    ClientEventType type();
}
