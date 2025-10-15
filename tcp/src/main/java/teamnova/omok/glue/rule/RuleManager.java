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
        return prepareRules(session, Collections.emptyMap(), pickDesiredRuleCount());
    }

    /**
     * Prepare rules once for the session using provided known scores.
     */
    public RulesContext prepareRules(GameSession session, Map<String, Integer> knownScores) {
        return prepareRules(session, knownScores, pickDesiredRuleCount());
    }

    /**
     * Core selection: choose desiredRuleCount among eligible rules where limitScore <= lowest participant score.
     */
    public RulesContext prepareRules(GameSession session,
                                     Map<String, Integer> knownScores,
                                     int desiredRuleCount) {
        Objects.requireNonNull(session, "session");
        Map<String, Integer> scores = resolveScores(session.getUserIds(), knownScores);
        int lowestScore = scores.values().stream().min(Integer::compareTo).orElse(0);
        List<Rule> candidates = registry.eligibleRules(lowestScore);
        if (candidates.isEmpty() || desiredRuleCount <= 0) {
            System.out.println("[RULE_LOG] No eligible rules for session " + session.getId());
            return RulesContext.fromRules(session, List.of(), lowestScore);
        }
        Collections.shuffle(candidates, random);
        int count = Math.min(Math.max(desiredRuleCount, 0), candidates.size());
        List<Rule> selected = new ArrayList<>(candidates.subList(0, count));
        System.out.println("[RULE_LOG] Selected " + selected.size() + " rules for session " + session.getId());
        return RulesContext.fromRules(session, selected, lowestScore);
    }

    private int pickDesiredRuleCount() {
        // 1..4 inclusive
        return random.nextInt(MAX_RULES - MIN_RULES + 1) + MIN_RULES;
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
