package teamnova.omok.glue.game.session.services;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionAccess;
import teamnova.omok.glue.rule.api.BoardSnapshotTransformingRule;
import teamnova.omok.glue.rule.runtime.GameSessionRuleBindings;

/**
 * Delegates snapshot rewriting to active rules that declare support for view
 * transformations. This keeps publisher code free from rule-specific branches
 * while still letting rules opt-in to presentation changes.
 */
public final class RuleAwareBoardSnapshotTransformer implements BoardSnapshotTransformer {
    @Override
    public byte[] transform(GameSessionAccess session, byte[] snapshot) {
        if (session == null || snapshot == null || snapshot.length == 0) {
            return snapshot;
        }
        GameSessionRuleBindings bindings = session.getRuleBindings();
        if (bindings == null) {
            return snapshot;
        }
        byte[] current = snapshot;
        for (BoardSnapshotTransformingRule transformingRule : bindings.capability(BoardSnapshotTransformingRule.class)) {
            current = transformingRule.transformBoardSnapshot(session, current);
            if (current == null) {
                break;
            }
        }
        return current;
    }
}
