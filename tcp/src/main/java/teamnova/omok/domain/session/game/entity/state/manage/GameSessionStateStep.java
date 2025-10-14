package teamnova.omok.domain.session.game.entity.state.manage;

import java.util.Objects;

/**
 * Represents the outcome of executing logic within a game session state,
 * indicating whether a transition should occur.
 */
public record GameSessionStateStep(GameSessionStateType nextState) {
    public static GameSessionStateStep stay() {
        return new GameSessionStateStep(null);
    }

    public static GameSessionStateStep transition(GameSessionStateType nextState) {
        Objects.requireNonNull(nextState, "nextState");
        return new GameSessionStateStep(nextState);
    }

    public boolean hasTransition() {
        return nextState != null;
    }
}
