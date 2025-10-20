package teamnova.omok.modules.matching.models;

/**
 * EkfhConfig holds all tunable parameters for the matching engine.
 * Defaults preserve the previous behavior unless otherwise noted.
 */
public class MatchingConfig {
    // Window expansion & time/credit weights
    public int baseMatchingGap = 500;            // was BASE_MATCHING_GAP (relaxed by +50) 우선 500으로 늘려놈
    public int timeMatchingWeight = 50;          // was TIME_MATCHING_WEIGHT
    public int creditMatchingWeight = 100;        // was CREDIT_MATCHING_WEIGHT

    // Group scoring weights (reduced influence to avoid overly long waits driven by score)
    public int groupBaseScore = 10_000;          // GROUP_BASE_SCORE
    public int groupStdDevNegativeWeight = -60;  // was -100, reduce penalty sensitivity
    public int groupMaxDeltaNegativeWeight = -1; // was -2, reduce delta penalty (still 0 within base gap)
    public int groupCreditPositiveWeight = 30;   // was 25, reduce positive boost
    public int groupHeadcountPositiveWeight = 30;// was 50, reduce positive boost

    public int limitGroupScore = 10_000;         // LIMIT_GROUP_SCORE

    public static MatchingConfig defaults() {
        return new MatchingConfig();
    }
}
