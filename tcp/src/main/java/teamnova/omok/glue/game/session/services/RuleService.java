package teamnova.omok.glue.game.session.services;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionRuleAccess;
import teamnova.omok.glue.game.session.model.PlayerResult;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContext;
import teamnova.omok.glue.rule.api.BoardSetupRule;
import teamnova.omok.glue.rule.api.BoardTransformRule;
import teamnova.omok.glue.rule.api.OutcomeResolution;
import teamnova.omok.glue.rule.api.OutcomeRule;
import teamnova.omok.glue.rule.api.Rule;
import teamnova.omok.glue.rule.api.RuleId;
import teamnova.omok.glue.rule.api.ParticipantOutcomeRule;
import teamnova.omok.glue.rule.api.TurnOrderRule;
import teamnova.omok.glue.rule.api.TurnBudgetRule;
import teamnova.omok.glue.rule.api.TurnTimingRule;
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

    public void setupBoard(GameSessionRuleAccess access, RuleRuntimeContext runtime) {
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
            if (rule instanceof BoardSetupRule setupRule) {
                setupRule.setupBoard(access, runtime);
            }
        }
    }

    public boolean adjustTurnTiming(GameSessionRuleAccess access, RuleRuntimeContext runtime) {
        Objects.requireNonNull(runtime, "runtime");
        if (access == null) {
            return false;
        }
        List<RuleId> ruleIds = access.getRuleIds();
        if (ruleIds == null || ruleIds.isEmpty()) {
            return false;
        }
        boolean changed = false;
        for (RuleId id : ruleIds) {
            Rule rule = RuleRegistry.getInstance().get(id);
            if (rule instanceof TurnTimingRule timingRule) {
                changed |= timingRule.adjustTurnTiming(access, runtime);
            }
        }
        return changed;
    }

    public boolean updateTurnBudget(GameSessionRuleAccess access, RuleRuntimeContext runtime) {
        Objects.requireNonNull(runtime, "runtime");
        if (access == null) {
            return false;
        }
        List<RuleId> ruleIds = access.getRuleIds();
        if (ruleIds == null || ruleIds.isEmpty()) {
            return false;
        }
        boolean changed = false;
        for (RuleId id : ruleIds) {
            Rule rule = RuleRegistry.getInstance().get(id);
            if (rule instanceof TurnBudgetRule budgetRule) {
                changed |= budgetRule.updateTurnBudget(access, runtime);
            }
        }
        return changed;
    }

    public boolean adjustTurnOrder(GameSessionRuleAccess access, RuleRuntimeContext runtime) {
        Objects.requireNonNull(runtime, "runtime");
        if (access == null) {
            return false;
        }
        List<RuleId> ruleIds = access.getRuleIds();
        if (ruleIds == null || ruleIds.isEmpty()) {
            return false;
        }
        boolean changed = false;
        for (RuleId id : ruleIds) {
            Rule rule = RuleRegistry.getInstance().get(id);
            if (rule instanceof TurnOrderRule orderRule) {
                changed |= orderRule.adjustTurnOrder(access, runtime);
            }
        }
        return changed;
    }

    public Map<String, PlayerResult> registerParticipantOutcomes(GameSessionRuleAccess access,
                                                                 RuleRuntimeContext runtime) {
        Objects.requireNonNull(runtime, "runtime");
        if (access == null) {
            return Map.of();
        }
        List<RuleId> ruleIds = access.getRuleIds();
        if (ruleIds == null || ruleIds.isEmpty()) {
            return Map.of();
        }
        Map<String, PlayerResult> assignments = new LinkedHashMap<>();
        for (RuleId id : ruleIds) {
            Rule rule = RuleRegistry.getInstance().get(id);
            if (rule instanceof ParticipantOutcomeRule participantRule) {
                Map<String, PlayerResult> additions = participantRule.registerParticipantOutcomes(access, runtime);
                if (additions == null || additions.isEmpty()) {
                    continue;
                }
                additions.forEach((userId, result) -> {
                    if (userId != null && result != null) {
                        assignments.put(userId, result);
                    }
                });
            }
        }
        if (assignments.isEmpty()) {
            return Map.of();
        }
        return Map.copyOf(assignments);
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

        //결과를 다루는것을 목적으로 하기때문에 결과에 접근하기 쉽게하기위해 직접 제공
        Map<String, PlayerResult> baseline = new LinkedHashMap<>();
        List<String> participants = runtime.stateContext().participants().getUserIds();
        if (participants != null) {
            for (String userId : participants) {
                if (userId != null) {
                    PlayerResult current = runtime.stateContext().outcomes().outcomeFor(userId);
                    baseline.put(userId, current);
                }
            }
        }
        Map<String, PlayerResult> working = new LinkedHashMap<>(baseline);
        boolean finalize = false;
        for (RuleId id : ruleIds) {
            Rule rule = RuleRegistry.getInstance().get(id);
            if (rule instanceof OutcomeRule outcomeRule) {
                OutcomeResolution snapshot = OutcomeResolution.of(working, finalize);
                Optional<OutcomeResolution> candidate = outcomeRule.resolveOutcome(
                    access,
                    runtime,
                    snapshot
                );
                if (candidate.isEmpty() || candidate.get().assignments().isEmpty()) {
                    continue;
                }
                OutcomeResolution resolution = candidate.get();
                resolution.assignments().forEach((userId, result) -> {
                    if (userId != null && result != null) {
                        working.put(userId, result);
                    }
                });
                finalize = finalize || resolution.finalizeNow();
            }
        }
        Map<String, PlayerResult> assignments = new LinkedHashMap<>();
        working.forEach((userId, result) -> {
            if (userId != null && result != null) {
                PlayerResult original = baseline.get(userId);
                if (!Objects.equals(original, result)) {
                    assignments.put(userId, result);
                }
            }
        });
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
