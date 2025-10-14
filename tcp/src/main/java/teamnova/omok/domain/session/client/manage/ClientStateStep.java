package teamnova.omok.domain.session.client.manage;

import java.util.Objects;

/**
 * Represents the outcome of executing client state logic,
 * indicating whether a transition should occur.
 */
public record ClientStateStep(ClientStateType nextState) {
    public static ClientStateStep stay() {
        return new ClientStateStep(null);
    }

    public static ClientStateStep transition(ClientStateType nextState) {
        Objects.requireNonNull(nextState, "nextState");
        return new ClientStateStep(nextState);
    }

    public boolean hasTransition() {
        return nextState != null;
    }
}
