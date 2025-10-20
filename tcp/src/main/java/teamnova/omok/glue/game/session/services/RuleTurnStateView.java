package teamnova.omok.glue.game.session.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import teamnova.omok.glue.game.session.interfaces.GameTurnService;
import teamnova.omok.glue.game.session.model.vo.TurnOrder;
import teamnova.omok.glue.game.session.states.manage.GameSessionStateContext;

/**
 * Aggregates immutable turn data snapshots for rule evaluation.
 */
public final class RuleTurnStateView {
    private final GameTurnService.TurnSnapshot currentSnapshot;
    private final GameTurnService.TurnSnapshot nextSnapshot;
    private final List<String> userOrder;
    private final List<String> activeUserIds;
    private final List<String> disconnectedUserIds;
    private final String actingUserId;
    private final int actingPlayerIndex;

    private RuleTurnStateView(GameTurnService.TurnSnapshot currentSnapshot,
                              GameTurnService.TurnSnapshot nextSnapshot,
                              List<String> userOrder,
                              List<String> activeUserIds,
                              List<String> disconnectedUserIds,
                              String actingUserId,
                              int actingPlayerIndex) {
        this.currentSnapshot = currentSnapshot;
        this.nextSnapshot = nextSnapshot;
        this.userOrder = userOrder;
        this.activeUserIds = activeUserIds;
        this.disconnectedUserIds = disconnectedUserIds;
        this.actingUserId = actingUserId;
        this.actingPlayerIndex = actingPlayerIndex;
    }

    public static RuleTurnStateView fromAdvance(GameTurnService.TurnSnapshot currentSnapshot,
                                                GameTurnService.TurnSnapshot nextSnapshot,
                                                List<String> userOrder,
                                                Set<String> disconnectedUserIds,
                                                String actingUserId,
                                                int actingPlayerIndex) {
        Objects.requireNonNull(userOrder, "userOrder");
        Objects.requireNonNull(disconnectedUserIds, "disconnectedUserIds");
        List<String> immutableOrder = Collections.unmodifiableList(new ArrayList<>(userOrder));
        List<String> disconnectedOrdered = immutableOrder.stream()
            .filter(disconnectedUserIds::contains)
            .toList();
        List<String> activeOrdered = immutableOrder.stream()
            .filter(id -> !disconnectedUserIds.contains(id))
            .toList();
        return new RuleTurnStateView(
            currentSnapshot,
            nextSnapshot,
            immutableOrder,
            Collections.unmodifiableList(activeOrdered),
            Collections.unmodifiableList(disconnectedOrdered),
            actingUserId,
            actingPlayerIndex
        );
    }

    public static RuleTurnStateView fromTurnStart(GameTurnService.TurnSnapshot snapshot,
                                                  List<String> userOrder,
                                                  Set<String> disconnectedUserIds) {
        Objects.requireNonNull(snapshot, "snapshot");
        Objects.requireNonNull(userOrder, "userOrder");
        Objects.requireNonNull(disconnectedUserIds, "disconnectedUserIds");
        return fromAdvance(
            null,
            snapshot,
            userOrder,
            disconnectedUserIds,
            null,
            -1
        );
    }

    public static RuleTurnStateView capture(GameSessionStateContext context,
                                            GameTurnService turnService) {
        Objects.requireNonNull(context, "context");
        Objects.requireNonNull(turnService, "turnService");
        GameTurnService.TurnSnapshot snapshot = turnService.snapshot(context.turns());
        TurnOrder order = context.turns().order();
        List<String> participantOrder = order != null
            ? order.userIds()
            : context.participants().getUserIds();
        return fromTurnStart(snapshot, participantOrder, context.participants().disconnectedUsersView());
    }

    public GameTurnService.TurnSnapshot currentSnapshot() {
        return currentSnapshot;
    }

    public GameTurnService.TurnSnapshot nextSnapshot() {
        return nextSnapshot;
    }

    public GameTurnService.TurnSnapshot resolvedSnapshot() {
        return nextSnapshot != null ? nextSnapshot : currentSnapshot;
    }

    public List<String> userOrder() {
        return userOrder;
    }

    public List<String> activeUserIds() {
        return activeUserIds;
    }

    public List<String> disconnectedUserIds() {
        return disconnectedUserIds;
    }

    public int activeUserCount() {
        return activeUserIds.size();
    }

    public int totalUserCount() {
        return userOrder.size();
    }

    public String actingUserId() {
        return actingUserId;
    }

    public int actingPlayerIndex() {
        return actingPlayerIndex;
    }

    public boolean roundWrapped() {
        return nextSnapshot != null && nextSnapshot.wrapped();
    }
}
