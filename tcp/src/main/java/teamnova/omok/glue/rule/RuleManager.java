package teamnova.omok.glue.rule;

import java.util.*;

import teamnova.omok.glue.service.MysqlService;
import teamnova.omok.glue.store.GameSession;

/**
 * Selects and prepares rules for a GameSession once at creation time.
 * Uses RuleRegistry and RuleBootstrap to discover available rules.
 */
public class RuleManager {
    public static final int MIN_RULES = 1;
    public static final int MAX_RULES = 4;
    private static final int DEFAULT_SCORE = 1000;

    private final MysqlService mysqlService; // optional, may be null in tests
    private final RuleRegistry registry;
    private final Random random = new Random();

    public RuleManager(MysqlService mysqlService) {
        this(mysqlService, RuleRegistry.getInstance());
    }

    RuleManager(MysqlService mysqlService, RuleRegistry registry) {
        this.mysqlService = mysqlService;
        this.registry = Objects.requireNonNull(registry, "registry");
        new RuleBootstrap().registerDefaults(this.registry);
    }

    /**
     * Prepare rules once for the session using DB scores.
     */
    public RulesContext prepareRules(GameSession session) {
        return prepareRules(session, Collections.emptyMap());
    }

    /**
     * Prepare rules once for the session using provided known scores.
     */
    public RulesContext prepareRules(GameSession session, Map<String, Integer> knownScores) {
        Objects.requireNonNull(session, "session");
        Map<String, Integer> scores = resolveScores(session.getUserIds(), knownScores);
        int lowestScore = scores.values().stream().min(Integer::compareTo).orElse(0);
        session.setLowestParticipantScore(lowestScore);
        int desiredRuleCount = deriveRuleCount(lowestScore);
        session.setDesiredRuleCount(desiredRuleCount);
        return selectAndBuildContext(session, lowestScore, desiredRuleCount);
    }

    private RulesContext selectAndBuildContext(GameSession session,
                                     int lowestScore,
                                     int desiredRuleCount) {
        List<Rule> candidates = registry.eligibleRules(lowestScore);
        if (candidates.isEmpty() || desiredRuleCount <= 0) {
            System.out.println("[RULE_LOG] No eligible rules for session " + session.getId());
            return RulesContext.fromRules(session, List.of(), lowestScore);
        }
        Collections.shuffle(candidates, random);
        int count = Math.min(Math.max(desiredRuleCount, 0), Math.min(candidates.size(), MAX_RULES));
        List<Rule> selected = new ArrayList<>(candidates.subList(0, count));
        System.out.println("[RULE_LOG] Selected " + selected.size() + " rules for session " + session.getId()
            + " (lowestScore=" + lowestScore + ", desired=" + desiredRuleCount + ")");
        return RulesContext.fromRules(session, selected, lowestScore);
    }

    private int deriveRuleCount(int lowestScore) {
        if (lowestScore <= 500) return 1;
        if (lowestScore <= 1000) return 2;
        if (lowestScore <= 2000) return 3;
        return 4; // 2000~2500 or above capped to max 4
    }

    private Map<String, Integer> resolveScores(List<String> userIds,
                                               Map<String, Integer> knownScores) {
        Map<String, Integer> resolved = new HashMap<>();
        for (String userId : userIds) {
            int score = DEFAULT_SCORE;
            if (knownScores != null && knownScores.containsKey(userId)) {
                score = knownScores.get(userId);
            } else if (mysqlService != null) {
                score = mysqlService.getUserScore(userId, DEFAULT_SCORE);
            }
            resolved.put(userId, score);
        }
        return resolved;
    }
}
