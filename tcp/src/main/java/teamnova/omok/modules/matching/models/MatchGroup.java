package teamnova.omok.modules.matching.models;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class MatchGroup {
    private final List<TicketInfo> tickets;
    private final int score;

    public MatchGroup(List<TicketInfo> tickets, int score) {
        this.tickets = List.copyOf(Objects.requireNonNull(tickets, "tickets"));
        this.score = score;
    }

    public List<MatchTicket> tickets() {
        return Collections.unmodifiableList(tickets.stream().map(TicketInfo::toMatchTicket).toList());
    }
    public int score() { return score; }
}
