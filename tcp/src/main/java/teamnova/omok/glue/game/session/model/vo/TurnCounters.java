package teamnova.omok.glue.game.session.model.vo;

/**
 * Immutable snapshot of turn counters for a session.
 */
public record TurnCounters(int actionNumber,
                           int roundNumber,
                           int positionInRound) {

    public static TurnCounters initial() {
        return new TurnCounters(0, 0, 0);
    }

    public static TurnCounters first() {
        return new TurnCounters(1, 1, 1);
    }

    public TurnCounters advance(boolean wrapped, int orderSize) {
        int nextAction = Math.max(1, actionNumber + 1);
        int nextRound = wrapped ? Math.max(1, roundNumber + 1) : Math.max(roundNumber, 1);
        int nextPosition;
        if (positionInRound <= 0) {
            nextPosition = Math.min(1, orderSize);
        } else if (wrapped) {
            nextPosition = orderSize <= 0 ? 0 : 1;
        } else {
            int candidate = positionInRound + 1;
            nextPosition = orderSize <= 0 ? 0 : Math.min(candidate, orderSize);
        }
        return new TurnCounters(nextAction, nextRound, nextPosition);
    }

    public TurnCounters advanceWithoutActive() {
        return new TurnCounters(Math.max(1, actionNumber + 1), Math.max(roundNumber, 1), 0);
    }
}

