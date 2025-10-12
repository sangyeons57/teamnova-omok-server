package teamnova.omok.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;

import teamnova.omok.rule.Rule;
import teamnova.omok.rule.RuleBootstrap;
import teamnova.omok.rule.RuleRegistry;
import teamnova.omok.rule.RulesContext;
import teamnova.omok.store.GameSession;

public class RuleService {
    public static final int DEFAULT_RULE_SELECTION_COUNT = 1;
    private static final int DEFAULT_SCORE = 1000;

    private final MysqlService mysqlService;
    private final RuleRegistry registry;
    private final Random random = new Random();

    public RuleService(MysqlService mysqlService) {
        this(mysqlService, RuleRegistry.getInstance());
    }

    RuleService(MysqlService mysqlService, RuleRegistry registry) {
        this.mysqlService = mysqlService;
        this.registry = registry;
        new RuleBootstrap().registerDefaults(registry);
    }

    public RulesContext prepareRules(GameSession session) {
        return prepareRules(session, Collections.emptyMap(), DEFAULT_RULE_SELECTION_COUNT);
    }

    public RulesContext prepareRules(GameSession session,
                                     Map<String, Integer> knownScores,
                                     int desiredRuleCount) {
        Objects.requireNonNull(session, "session");
        Map<String, Integer> scores = resolveScores(session.getUserIds(), knownScores);
        int lowestScore = scores.values().stream().min(Integer::compareTo).orElse(0);
        List<Rule> candidates = registry.eligibleRules(lowestScore);
        if (candidates.isEmpty() || desiredRuleCount <= 0) {
            return RulesContext.fromRules(session, List.of(), lowestScore);
        }
        Collections.shuffle(candidates, random);
        int count = Math.min(Math.max(desiredRuleCount, 0), candidates.size());
        List<Rule> selected = new ArrayList<>(candidates.subList(0, count));
        return RulesContext.fromRules(session, selected, lowestScore);
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
