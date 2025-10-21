package teamnova.omok.glue.rule;

import java.util.*;

import teamnova.omok.glue.manager.DataManager;
import teamnova.omok.glue.game.session.model.GameSession;

/**
 * Selects and prepares rules for a GameSession once at creation time.
 * Uses RuleRegistry and RuleBootstrap to discover available rules.
 */
public class RuleManager {
    public static final int MIN_RULES = 1;
    public static final int MAX_RULES = 4;
    private static final int DEFAULT_SCORE = 1000;
    private static RuleManager INSTANCE;

    public static RuleManager Init(RuleRegistry registry) {
        INSTANCE = new RuleManager(registry);
        return INSTANCE;
    }

    public static RuleManager getInstance() {
        if( INSTANCE == null) {
            throw new IllegalStateException("RuleManager not initialized");
        }
        return INSTANCE;
    }

    private final RuleRegistry registry;
    private final Random random = new Random();
    private final RuleSelectionConfig selectionConfig;

    private RuleManager(RuleRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.selectionConfig = RuleSelectionConfig.load();
        new RuleBootstrap().registerDefaults(this.registry);
    }

    /**
     * Prepare rules once for the session using DB scores.
     */
    public List<RuleId> prepareRules() {
        return prepareRules(Collections.emptyMap());
    }

    /**
     * Prepare rules once for the session using provided known scores.
     */
    public List<RuleId> prepareRules(Map<String, Integer> knownScores) {
        int lowestScore = knownScores.values().stream().min(Integer::compareTo).orElse(0);
        int desiredRuleCount = selectionConfig.hasFixedRuleOverride()
            ? selectionConfig.fixedRuleIds().size()
            : deriveRuleCount(lowestScore);
        return selectAndBuildContext(lowestScore, desiredRuleCount);
    }

    private List<RuleId> selectAndBuildContext(int lowestScore, int desiredRuleCount) {
        if (selectionConfig.hasFixedRuleOverride()) {
            List<Rule> fixed = selectionConfig.resolveFixedRules(registry);
            if (!fixed.isEmpty()) {
                return selectionConfig.fixedRuleIds();
            }
            int fallbackCount = deriveRuleCount(lowestScore);
            desiredRuleCount = fallbackCount;
        }
        List<Rule> candidates = registry.eligibleRules(lowestScore);
        if (candidates.isEmpty() || desiredRuleCount <= 0) {
            return List.of();
        }
        Collections.shuffle(candidates, random);
        int count = Math.min(Math.max(desiredRuleCount, 0), Math.min(candidates.size(), MAX_RULES));
        List<Rule> selected = new ArrayList<>(candidates.subList(0, count));

        return selected.stream().map(rule -> rule.getMetadata().id).toList();
    }

    private int deriveRuleCount(int lowestScore) {
        if (lowestScore < 500) return 1;
        if (lowestScore < 1000) return 2;
        if (lowestScore < 2000) return 3;
        return 4;
    }
}
