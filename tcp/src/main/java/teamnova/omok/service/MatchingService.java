package teamnova.omok.service;

import java.time.Instant;
import java.util.*;

public class MatchingService {
    private final Queue<Ticket> globalQueue;
    private final HashMap<Integer, List<Ticket>> ticketGroup ;
    public MatchingService() {
        this.globalQueue = new java.util.concurrent.ConcurrentLinkedQueue<>();
        this.ticketGroup = new HashMap<>();
        this.ticketGroup.put(2, new ArrayList<>());
        this.ticketGroup.put(3, new ArrayList<>());
        this.ticketGroup.put(4, new ArrayList<>());
    }

    public boolean enqueue(Ticket ticket) {
        boolean isSuccess = globalQueue.offer(ticket);

        if (isSuccess){
            for (int number : ticket.matchSet) ticketGroup.get(number).add(ticket);
        }

        return isSuccess;
    }

    public Result tryMatch() {
        Ticket ticket = globalQueue.poll();
        if (ticket == null) return Result.fail("No ticket available");

        Group bestGroup = null;
        for (int match : ticket.matchSet) {
            List<Ticket> neighborTickets = getNeighborTickets(ticketGroup.get(match), ticket);
            Group group = buildGroup(neighborTickets, ticket);

            if (bestGroup == null || group.getScore() > bestGroup.getScore()) {
                bestGroup = group;
            }
        }

        if (bestGroup != null){
            // 티켓 선택완료
            return Result.success(bestGroup);
        }
        return Result.fail("No group available");
    }

    public List<Ticket> getNeighborTickets(List<Ticket> selectedGroup, Ticket selectedTicket) {

    }

    public Group buildGroup(List<Ticket> neighborTickets, Ticket selectedTicket) {

    }

    public static class Ticket {
        public Ticket(String userId, int rating, Set<Integer> matchSet) {
            this.id = UUID.randomUUID();
            this.timestamp = System.currentTimeMillis();
            this.userId = userId;
            this.rating = rating;
            this.matchSet = matchSet;
            this.credit = 10;
            this.state = MatchingTicketState.CREATED;
        }

        public final UUID id;
        public final String userId;
        public final long timestamp;
        public final int rating;
        public final Set<Integer> matchSet;
        public MatchingTicketState state;
        public int credit;

        public static enum MatchingTicketState {
            CREATED,
        }
    }

    public static class Group {
        private final List<Ticket> tickets;
        private final int score;
        public Group(List<Ticket> tickets, int score) {
            Collections.copy(tickets, tickets);
            this.tickets = tickets;
            this.score = score;
        }

        public int getScore() {
            return score;
        }
        public List<Ticket> getTickets() {
            return tickets;
        }
    }

    public interface Result {
        // 매칭 성공
        public record Success(Group group) implements Result {}
        // 매칭 실패
        public record Fail(String message) implements Result {}

        private static Fail fail(String message) {
            return new Fail(message);
        }

        private static Success success(Group group) {
            return new Success(group);
        }
    }

}
