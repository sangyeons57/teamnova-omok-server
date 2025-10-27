package teamnova.omok.glue.rule.api;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionRuleAccess;

/**
 * Optional capability for rules that need to rewrite board snapshots before
 * they are broadcast to players. Implementations can adjust the byte array to
 * match the rule's presentation requirements while keeping publishers free of
 * rule-specific branches.
 */
public interface BoardTransformRule {
    byte[] transformBoard(GameSessionRuleAccess access, byte[] snapshot);
}
