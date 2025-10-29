package teamnova.omok.glue.rule.runtime;

/**
 * Centralized constants for {@link teamnova.omok.glue.game.session.interfaces.session.GameSessionRuleAccess}
 * data keys used by rule capability implementations.
 */
public final class RuleDataKeys {
    private RuleDataKeys() {
    }

    public static final String DELAYED_REVEAL_QUEUE = "rules.delayedReveal.queue";
    public static final String DELAYED_REVEAL_SCHEDULE = "rules.delayedReveal.schedule";
    public static final String EVOLUTION_AGE_MAP = "rules.evolution.ageMap";
    public static final String LOW_DENSITY_LAST_ROUND = "rules.lowDensity.lastRound";
    public static final String ROUND_TRIP_DIRECTION = "rules.roundTrip.direction";
    public static final String SPEED_GAME_BUDGETS = "rules.speedGame2.budgets";
    public static final String SIX_IN_ROW_LAST_TURN = "rules.sixInRow.lastTurn";
    public static final String TEN_CHAIN_LAST_TURN = "rules.tenChain.lastTurn";
    public static final String ROUND_TRIP_LAST_ROUND = "rules.roundTrip.lastRound";
    public static final String SHUFFLE_LAST_ROUND = "rules.turnOrderShuffle.lastRound";
    public static final String SPEED_GAME_LAST_PLAYER = "rules.speedGame2.lastPlayer";
    public static final String SPEED_GAME_LAST_TURN_START = "rules.speedGame2.lastStart";
}
