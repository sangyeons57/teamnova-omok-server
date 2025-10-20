package teamnova.omok.modules.matching.models;

/**
 * EkfhConfig holds all tunable parameters for the matching engine.
 * Defaults preserve the previous behavior unless otherwise noted.
 */
public class MatchingConfig {
    // Window expansion & time/credit weights
    public int baseMatchingGap = 150;            // was BASE_MATCHING_GAP (relaxed by +50)
    public int timeMatchingWeight = 20;          // was TIME_MATCHING_WEIGHT
    public int creditMatchingWeight = 80;        // was CREDIT_MATCHING_WEIGHT

    // Group scoring weights (reduced influence to avoid overly long waits driven by score)
    public int groupBaseScore = 10_000;          // GROUP_BASE_SCORE
    public int groupStdDevNegativeWeight = -60;  // was -100, reduce penalty sensitivity
    public int groupMaxDeltaNegativeWeight = -1; // was -2, reduce delta penalty (still 0 within base gap)
    public int groupCreditPositiveWeight = 15;   // was 25, reduce positive boost
    public int groupHeadcountPositiveWeight = 30;// was 50, reduce positive boost

    public int limitGroupScore = 10_000;         // LIMIT_GROUP_SCORE

    public static MatchingConfig defaults() {
        return new MatchingConfig();
    }
}
