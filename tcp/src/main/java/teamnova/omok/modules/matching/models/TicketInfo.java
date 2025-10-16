package teamnova.omok.modules.matching.models;

import java.util.Objects;
import java.util.Set;

public class TicketInfo {
    private final String id;
    private final int rating;
    private final Set<Integer> matchSet;
    private final long timestamp;
    private int credit;

    TicketInfo(String id, int rating, Set<Integer> matchSet) {
        this.id = Objects.requireNonNull(id, "id");
        this.rating = rating;
        this.matchSet = Set.copyOf(Objects.requireNonNull(matchSet, "matchSet"));
        this.timestamp = System.currentTimeMillis();
        this.credit = 0;
    }

    public static TicketInfo create(MatchTicket ticket) {
        return new TicketInfo(ticket.id(), ticket.rating(), ticket.matchSet());
    }

    public MatchTicket toMatchTicket() {
        return MatchTicket.create(id, rating, matchSet);
    }

    public void addCredit() { this.credit++; }

    public String getId() {
        return id;
    }

    public int getCredit() {
        return credit;
    }

    public int getRating() {
        return rating;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Set<Integer> getMatchSet() {
        return matchSet;
    }
}
