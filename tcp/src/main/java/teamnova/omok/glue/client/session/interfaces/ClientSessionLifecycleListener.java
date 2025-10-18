package teamnova.omok.glue.client.session.interfaces;

/**
 * Callback interface for session lifecycle events. Implemented by the manager layer.
 */
public interface ClientSessionLifecycleListener {
    void onSessionClosed(ClientSessionHandle session);
}

