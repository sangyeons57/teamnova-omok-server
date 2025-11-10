package teamnova.omok.glue.client.state;

import java.util.Objects;
import java.util.Set;

/**
 * Thin command facade for submitting client-state events. Centralizes the mapping between
 * high-level operations and the underlying state-machine events so ClientStateHub stays lean.
 */
public final class ClientStateCommandBus {
    private final ClientStateHub hub;

    public ClientStateCommandBus(ClientStateHub hub) {
        this.hub = Objects.requireNonNull(hub, "hub");
    }

    public void markAuthenticated() {
        hub.submit(ClientStateEvents.authenticated());
    }

    public void disconnect() {
        hub.submit(ClientStateEvents.disconnect());
    }

    public void resetToConnected() {
        hub.submit(ClientStateEvents.reset());
    }

    public void requestMatchmaking(long requestId, int rating, Set<Integer> matchSizes) {
        if (matchSizes == null || matchSizes.isEmpty()) {
            return;
        }
        hub.submit(ClientStateEvents.startMatching(requestId, rating, matchSizes));
    }

    public void cancelMatchmaking(long requestId) {
        hub.submit(ClientStateEvents.cancelMatching(requestId));
    }
}
