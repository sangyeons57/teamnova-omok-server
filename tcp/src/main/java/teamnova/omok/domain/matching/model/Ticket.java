package teamnova.omok.domain.matching.model;

import java.util.Set;

public class Ticket {
    public Ticket(String userId, int rating, Set<Integer> matchSet) {
        this.id = userId;
        this.timestamp = System.currentTimeMillis();
        this.rating = rating;
        this.matchSet = matchSet;
        this.credit = 0;
        this.state = MatchingService.Ticket.MatchingTicketState.CREATED;
    }

    public final String id;
    public final long timestamp;
    public final int rating;
    public final Set<Integer> matchSet;
    public MatchingService.Ticket.MatchingTicketState state;
    private int credit;

    public static enum MatchingTicketState {
        CREATED,
    }

    public int getCredit() {
        return credit;
    }
    public void addCredit() {
        this.credit++;
    }
}
