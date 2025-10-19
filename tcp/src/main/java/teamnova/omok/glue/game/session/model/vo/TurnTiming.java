package teamnova.omok.glue.game.session.model.vo;

/**
 * Value object describing the temporal window of a turn.
 */
public record TurnTiming(long startAt, long endAt) {

    public static TurnTiming idle() {
        return new TurnTiming(0L, 0L);
    }

    public static TurnTiming of(long startAt, long endAt) {
        return new TurnTiming(startAt, endAt);
    }

    public boolean isExpired(long now) {
        return endAt > 0 && now >= endAt;
    }
}

