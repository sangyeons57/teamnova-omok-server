package teamnova.omok.glue.game.session.services;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import teamnova.omok.glue.game.session.model.Stone;
import teamnova.omok.support.TestRuleAccess;

class HiddenPlacementCoordinatorTest {

    @Test
    void queueAndDrainRemovesFromBuffer() {
        HiddenPlacementCoordinator coordinator = new HiddenPlacementCoordinator();
        TestRuleAccess access = new TestRuleAccess();
        HiddenPlacementCoordinator.HiddenPlacement placement =
            new HiddenPlacementCoordinator.HiddenPlacement("user-a", 4, 5, 45, Stone.PLAYER1, 10L, 42L, 12);

        coordinator.queue(access, placement);

        Assertions.assertTrue(coordinator.hasHiddenPlacements(access));
        List<HiddenPlacementCoordinator.HiddenPlacement> drained = coordinator.drain(access);
        Assertions.assertEquals(1, drained.size());
        Assertions.assertEquals(placement, drained.getFirst());
        Assertions.assertFalse(coordinator.hasHiddenPlacements(access));
    }

    @Test
    void clearRemovesQueuedPlacements() {
        HiddenPlacementCoordinator coordinator = new HiddenPlacementCoordinator();
        TestRuleAccess access = new TestRuleAccess();
        coordinator.queue(access, new HiddenPlacementCoordinator.HiddenPlacement("user-b", 1, 2, 21, Stone.PLAYER2, 11L, 99L, 7));

        coordinator.clear(access);

        Assertions.assertFalse(coordinator.hasHiddenPlacements(access));
        Assertions.assertTrue(coordinator.drain(access).isEmpty());
    }
}
