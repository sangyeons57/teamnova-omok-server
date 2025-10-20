package teamnova.omok.glue.rule;

/**
 * Identifies which lifecycle moment triggered the current rule invocation.
 */
public enum RuleTriggerKind {
    TURN_ADVANCE,
    TURN_ROUND_COMPLETED,
    TURN_START
}
