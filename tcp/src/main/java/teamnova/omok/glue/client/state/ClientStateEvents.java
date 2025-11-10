package teamnova.omok.glue.client.state;

import java.util.Collections;
import java.util.Set;

import teamnova.omok.glue.client.state.event.AuthenticatedClientEvent;
import teamnova.omok.glue.client.state.event.CancelMatchingClientEvent;
import teamnova.omok.glue.client.state.event.DisconnectClientEvent;
import teamnova.omok.glue.client.state.event.ResetClientEvent;
import teamnova.omok.glue.client.state.event.StartMatchingClientEvent;
import teamnova.omok.modules.state_machine.interfaces.BaseEvent;

/**
 * Factory helpers for building client-state events so callers do not need to touch concrete types.
 */
public final class ClientStateEvents {
    private ClientStateEvents() { }

    public static BaseEvent authenticated() {
        return new AuthenticatedClientEvent();
    }

    public static BaseEvent disconnect() {
        return new DisconnectClientEvent();
    }

    public static BaseEvent reset() {
        return new ResetClientEvent();
    }

    public static BaseEvent startMatching(long requestId, int rating, Set<Integer> matchSizes) {
        Set<Integer> safeSet = matchSizes == null ? Collections.emptySet() : matchSizes;
        return new StartMatchingClientEvent(safeSet, rating, requestId);
    }

    public static BaseEvent cancelMatching(long requestId) {
        return new CancelMatchingClientEvent(requestId);
    }
}
