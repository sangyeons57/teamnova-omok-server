package teamnova.omok.rule;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

import teamnova.omok.state.game.manage.GameSessionStateContext;
import teamnova.omok.state.game.manage.GameSessionStateType;
import teamnova.omok.store.GameSession;

public class RulesContext {
    private final GameSession session;
    private final Map<RuleType, List<RuleId>> ruleMap;
    private final int lowestScore;
    private final Map<String, Object> data = new ConcurrentHashMap<>();

    private volatile RuleType currentType = RuleType.UNKNOWN;
    private volatile GameSessionStateContext stateContext;

    private RulesContext(GameSession session,
                         Map<RuleType, List<RuleId>> ruleMap,
                         int lowestScore) {
        this.session = Objects.requireNonNull(session, "session");
        this.ruleMap = new EnumMap<>(RuleType.class);
        ruleMap.forEach((type, ids) -> this.ruleMap.put(type, List.copyOf(ids)));
        this.lowestScore = lowestScore;
    }

    public static RulesContext fromRules(GameSession session,
                                         List<Rule> rules,
                                         int lowestScore) {
        Objects.requireNonNull(session, "session");
        Objects.requireNonNull(rules, "rules");
        Map<RuleType, List<RuleId>> mapping = new EnumMap<>(RuleType.class);
        for (Rule rule : rules) {
            RuleMetadata metadata = rule.getMetadata();
            if (metadata == null || metadata.id == null || metadata.types == null) {
                continue;
            }
            for (RuleType type : metadata.types) {
                if (type == null || type == RuleType.UNKNOWN) {
                    continue;
                }
                mapping.computeIfAbsent(type, ignored -> new ArrayList<>()).add(metadata.id);
            }
        }
        return new RulesContext(session, mapping, lowestScore);
    }

    public GameSession getSession() {
        return session;
    }

    public Object getData(String key) {
        return data.get(key);
    }

    public int lowestScore() {
        return lowestScore;
    }

    public void putData(String key, Object value) {
        data.put(key, value);
    }

    public GameSessionStateContext stateContext() {
        return stateContext;
    }

    public void attachStateContext(GameSessionStateContext stateContext) {
        this.stateContext = stateContext;
    }

    public boolean setCurrentRuleByGameState(GameSessionStateType type) {
        RuleType ruleType = switch (type) {
            case LOBBY -> RuleType.LOBBY;
            case TURN_WAITING -> RuleType.TURN_WAITING;
            case MOVE_VALIDATING -> RuleType.MOVE_VALIDATING;
            case MOVE_APPLYING -> RuleType.MOVE_APPLYING;
            case OUTCOME_EVALUATING -> RuleType.OUTCOME_EVALUATING;
            case TURN_FINALIZING -> RuleType.TURN_FINALIZING;
            case POST_GAME_DECISION_WAITING -> RuleType.POST_GAME_DECISION_WAITING;
            case POST_GAME_DECISION_RESOLVING -> RuleType.POST_GAME_DECISION_RESOLVING;
            case SESSION_REMATCH_PREPARING -> RuleType.SESSION_REMATCH_PREPARING;
            case SESSION_TERMINATING -> RuleType.SESSION_TERMINATING;
            case COMPLETED -> RuleType.COMPLETED;
        };
        this.currentType = ruleType;
        return !getCurrentRuleIds().isEmpty();
    }

    private List<RuleId> getCurrentRuleIds() {
        return ruleMap.getOrDefault(currentType, List.of());
    }

    private void activateRule(RuleId id) {
        if (id == null) {
            return;
        }
        Rule rule = RuleRegistry.getInstance().get(id);
        if (rule == null) {
            return;
        }
        rule.invoke(this);
    }

    public void activateCurrentRule() {
        for (RuleId id : getCurrentRuleIds()) {
            activateRule(id);
        }
    }
}
