package teamnova.omok.glue.client.session.interfaces;

/**
 * Receives state transition notifications for managed client sessions.
 */
@FunctionalInterface
public interface ClientSessionStateListener {
    void onStateChanged(ClientSessionHandle session);
}
