package teamnova.omok.state.client.event;

/**
 * Marker for events routed through {@link teamnova.omok.state.client.manage.ClientStateManager}.
 */
public interface ClientEvent {
    ClientEventType type();
}
