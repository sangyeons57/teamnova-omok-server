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
import teamnova.omok.glue.rule.api.BoardSweepRule;
import teamnova.omok.glue.rule.api.BoardTransformRule;
import teamnova.omok.glue.rule.api.MoveMutationRule;
import teamnova.omok.glue.rule.api.OutcomeResolution;
import teamnova.omok.glue.rule.api.OutcomeRule;
import teamnova.omok.glue.rule.api.Rule;
import teamnova.omok.glue.rule.api.RuleId;
import teamnova.omok.glue.rule.api.ParticipantOutcomeRule;
import teamnova.omok.glue.rule.api.TurnOrderRule;
import teamnova.omok.glue.rule.api.TurnBudgetRule;
import teamnova.omok.glue.rule.api.TurnLifecycleRule;
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

    public void applyMoveMutations(GameSessionRuleAccess access, RuleRuntimeContext runtime) {
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
            if (rule instanceof MoveMutationRule mutationRule) {
                mutationRule.applyMoveMutation(access, runtime);
            }
        }
    }

    public void tickTurnLifecycle(GameSessionRuleAccess access, RuleRuntimeContext runtime) {
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
            if (rule instanceof TurnLifecycleRule lifecycleRule) {
                lifecycleRule.onTurnTick(access, runtime);
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
                changed |= runtime.services().turnOrderCoordinator().apply(orderRule, access, runtime);
            }
        }
        return changed;
    }

    public void performBoardSweep(GameSessionRuleAccess access, RuleRuntimeContext runtime) {
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
            if (rule instanceof BoardSweepRule sweepRule) {
                sweepRule.sweepBoard(access, runtime);
            }
        }
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
