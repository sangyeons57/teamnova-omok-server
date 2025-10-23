package teamnova.omok.glue.rule.api;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionRuleAccess;

/**
 * Marker contract for rules that need to rewrite board snapshots before they
 * are broadcast to players. Implementations can adjust the byte array to match
 * the rule's presentation requirements.
 */
public interface BoardSnapshotTransformingRule {
    byte[] transformBoardSnapshot(GameSessionRuleAccess access, byte[] snapshot);
}
