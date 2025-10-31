package teamnova.omok.glue.rule.api;

import java.util.List;
import java.util.Objects;

/**
 * Represents a candidate turn order mutation flowing through the rule pipeline.
 * Rules may return a new instance with modified ordering.
 */
public record TurnOrderAdjustment(List<String> order) {
    public TurnOrderAdjustment {
        Objects.requireNonNull(order, "order");
        if (order.isEmpty()) {
            throw new IllegalArgumentException("order must not be empty");
        }
        order = List.copyOf(order);
    }

    public static TurnOrderAdjustment of(List<String> order) {
        return new TurnOrderAdjustment(order);
    }

    public TurnOrderAdjustment withOrder(List<String> newOrder) {
        return new TurnOrderAdjustment(newOrder);
    }
}
