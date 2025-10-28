package teamnova.omok.glue.game.session.services;

import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import teamnova.omok.support.TestRuleAccess;

class TurnBudgetManagerTest {

    @Test
    void updateAndSnapshotExposeCurrentBudgets() {
        TurnBudgetManager manager = new TurnBudgetManager();
        TestRuleAccess access = new TestRuleAccess();

        manager.update(access, "alice", 1000L);
        manager.update(access, "bob", 500L);

        Map<String, Long> snapshot = manager.snapshot(access);
        Assertions.assertEquals(2, snapshot.size());
        Assertions.assertEquals(1_000L, snapshot.get("alice"));
        Assertions.assertEquals(500L, snapshot.get("bob"));
        Assertions.assertEquals(1_000L, manager.remaining(access, "alice"));
    }

    @Test
    void decrementClampsToZero() {
        TurnBudgetManager manager = new TurnBudgetManager();
        TestRuleAccess access = new TestRuleAccess();

        manager.update(access, "alice", 300L);
        long remaining = manager.decrement(access, "alice", 500L);

        Assertions.assertEquals(0L, remaining);
        Assertions.assertEquals(0L, manager.remaining(access, "alice"));
    }
}
