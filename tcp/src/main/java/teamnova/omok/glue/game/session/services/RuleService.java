package teamnova.omok.glue.game.session.services;

import java.util.*;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionRuleAccess;
import teamnova.omok.glue.game.session.model.PlayerResult;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContext;
import teamnova.omok.glue.rule.api.BoardTransformRule;
import teamnova.omok.glue.rule.api.OutcomeResolution;
import teamnova.omok.glue.rule.api.OutcomeRule;
import teamnova.omok.glue.rule.api.Rule;
import teamnova.omok.glue.rule.api.RuleId;
import teamnova.omok.glue.rule.runtime.RuleRegistry;
import teamnova.omok.glue.rule.runtime.RuleRuntimeContext;

public class RuleService {
    private static RuleService INSTANCE;

    public static synchronized RuleService Init() {
        INSTANCE = new RuleService();
        return INSTANCE;
    }

    public static RuleService getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new RuleService();
        }
        return INSTANCE;
    }

    private RuleService() {
    }

    public void activateRules(GameSessionRuleAccess access, RuleRuntimeContext runtime) {
        Objects.requireNonNull(runtime, "runtime");
        if (access == null) {
            return;
        }
        List<RuleId> ruleIds = access.getRuleIds();
        if (ruleIds == null || ruleIds.isEmpty()) {
            return;
        }
        for (RuleId id : ruleIds) {
            Rule rule = RuleRegistry.getInstance().get(id);
            if (rule != null) {
                rule.invoke(access, runtime);
            }
        }
    }

    public byte[] transformBoard(GameSessionRuleAccess access, byte[] snapshot) {
        if (access == null || snapshot == null || snapshot.length == 0) {
            return snapshot;
        }
        List<RuleId> ruleIds = access.getRuleIds();
        if (ruleIds == null || ruleIds.isEmpty()) {
            return snapshot;
        }
        byte[] current = Arrays.copyOf(snapshot, snapshot.length);
        for (RuleId id : ruleIds) {
            Rule rule = RuleRegistry.getInstance().get(id);
            if (rule instanceof BoardTransformRule transformRule) {
                byte[] next = transformRule.transformBoard(access, current);
                if (next == null) {
                    break;
                }
                current = next;
            }
        }
        return current;
    }

    public Optional<OutcomeResolution> resolveOutcome(GameSessionRuleAccess access, RuleRuntimeContext runtime) {
        Objects.requireNonNull(runtime, "runtime");
        if (access == null) {
            return Optional.empty();
        }
        List<RuleId> ruleIds = access.getRuleIds();
        if (ruleIds == null || ruleIds.isEmpty()) {
            return Optional.empty();
        }
        Map<String, PlayerResult> assignments = new LinkedHashMap<>();
        boolean finalize = false;
        for (RuleId id : ruleIds) {
            Rule rule = RuleRegistry.getInstance().get(id);
            if (rule instanceof OutcomeRule outcomeRule) {
                Optional<OutcomeResolution> candidate = outcomeRule.resolveOutcome(access, runtime);
                if (candidate.isEmpty()) {
                    continue;
                }
                OutcomeResolution resolution = candidate.get();
                if (!resolution.assignments().isEmpty()) {
                    assignments.putAll(resolution.assignments());
                }
                finalize = finalize || resolution.finalizeNow();
            }
        }
        if (assignments.isEmpty() && !finalize) {
            return Optional.empty();
        }
        return Optional.of(OutcomeResolution.of(assignments, finalize));
    }

    public Optional<OutcomeResolution> applyOutcomeRules(GameSessionStateContext context,
                                                         RuleRuntimeContext runtime) {
        Objects.requireNonNull(context, "context");
        Optional<OutcomeResolution> resolution = resolveOutcome(context.rules(), runtime);
        resolution.ifPresent(res -> {
            res.assignments().forEach((userId, result) -> {
                if (userId != null && result != null) {
                    context.outcomes().updateOutcome(userId, result);
                }
            });
            if (res.finalizeNow()) {
                context.participants().getUserIds().forEach(userId -> {
                    PlayerResult current = context.outcomes().outcomeFor(userId);
                    if (current == null || current == PlayerResult.PENDING) {
                        context.outcomes().updateOutcome(userId, PlayerResult.LOSS);
                    }
                });
            }
        });
        return resolution;
    }
}
