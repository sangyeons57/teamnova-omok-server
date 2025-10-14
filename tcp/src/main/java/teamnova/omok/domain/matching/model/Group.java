package teamnova.omok.domain.matching.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Group {
    private final List<Ticket> tickets;
    private final int score;
    public Group(List<Ticket> tickets, int score) {
        this.tickets = Collections.unmodifiableList(new ArrayList<>(tickets));
        this.score = score;
    }

    public int getScore() {
        return score;
    }
    public List<Ticket> getTickets() {
        return tickets;
    }
}
