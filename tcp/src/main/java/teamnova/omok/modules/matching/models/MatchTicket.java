package teamnova.omok.modules.matching.models;

import java.util.Objects;
import java.util.Set;

/**
 * Immutable DTO for a match ticket.
 */
public final class MatchTicket {
    private final String id;
    private final int rating;
    private final Set<Integer> matchSet;

    private MatchTicket(String id, int rating, Set<Integer> matchSet) {
        this.id = Objects.requireNonNull(id, "id");
        this.rating = rating;
        this.matchSet = Set.copyOf(Objects.requireNonNull(matchSet, "matchSet"));
    }

    public static MatchTicket create(String id, int rating, Set<Integer> matchSet) {
        return new MatchTicket(id, rating, matchSet);
    }


    public String id() { return id; }
    public int rating() { return rating; }
    public Set<Integer> matchSet() { return matchSet; }
}
