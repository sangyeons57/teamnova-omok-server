package teamnova.omok.glue.client.session.interfaces;

import teamnova.omok.glue.client.session.states.manage.ClientStateType;

/**
 * Receives state transition notifications for managed client sessions.
 */
@FunctionalInterface
public interface ClientSessionStateListener {
    void onStateChanged(ClientSessionHandle session, ClientStateType previous, ClientStateType current);
}
