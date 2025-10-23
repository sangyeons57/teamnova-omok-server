package teamnova.omok.glue.game.session.services;

import java.util.List;
import java.util.Objects;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionAccess;
import teamnova.omok.glue.rule.BoardSnapshotTransformingRule;
import teamnova.omok.glue.rule.Rule;
import teamnova.omok.glue.rule.RuleId;
import teamnova.omok.glue.rule.RuleRegistry;

/**
 * Delegates snapshot rewriting to active rules that declare support for view
 * transformations. This keeps publisher code free from rule-specific branches
 * while still letting rules opt-in to presentation changes.
 */
public final class RuleAwareBoardSnapshotTransformer implements BoardSnapshotTransformer {
    private final RuleRegistry ruleRegistry;

    public RuleAwareBoardSnapshotTransformer(RuleRegistry ruleRegistry) {
        this.ruleRegistry = Objects.requireNonNull(ruleRegistry, "ruleRegistry");
    }

    @Override
    public byte[] transform(GameSessionAccess session, byte[] snapshot) {
        if (session == null || snapshot == null || snapshot.length == 0) {
            return snapshot;
        }
        List<RuleId> ruleIds = session.getRuleIds();
        if (ruleIds == null || ruleIds.isEmpty()) {
            return snapshot;
        }
        byte[] current = snapshot;
        for (RuleId ruleId : ruleIds) {
            Rule rule = ruleRegistry.get(ruleId);
            if (rule instanceof BoardSnapshotTransformingRule transformingRule) {
                current = transformingRule.transformBoardSnapshot(session, current);
                if (current == null) {
                    break;
                }
            }
        }
        return current;
    }
}
