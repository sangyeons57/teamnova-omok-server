package teamnova.omok.state.client.manage;

import java.util.Objects;

/**
 * Represents the result of executing client state logic, optionally requesting
 * a transition to another state.
 *
 * @param <T> payload emitted by the state operation
 */
public record ClientStateStep<T>(T payload, ClientStateType nextState) {
    public static <T> ClientStateStep<T> stay(T payload) {
        return new ClientStateStep<>(payload, null);
    }

    public static <T> ClientStateStep<T> transition(T payload, ClientStateType nextState) {
        Objects.requireNonNull(nextState, "nextState");
        return new ClientStateStep<>(payload, nextState);
    }

    public boolean hasTransition() {
        return nextState != null;
    }
}
