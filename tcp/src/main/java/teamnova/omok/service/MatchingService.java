package teamnova.omok.service;

import java.util.*;

public class MatchingService {
    private final Queue<Ticket> globalQueue;
    private final HashMap<Integer, List<Ticket>> ticketGroups;
    public MatchingService() {
        this.globalQueue = new java.util.concurrent.ConcurrentLinkedQueue<>();
        this.ticketGroups = new HashMap<>();
        this.ticketGroups.put(2, new ArrayList<>());
        this.ticketGroups.put(3, new ArrayList<>());
        this.ticketGroups.put(4, new ArrayList<>());
    }

    public boolean enqueue(Ticket ticket) {
        boolean isSuccess = globalQueue.offer(ticket);

        if (isSuccess){
            for (int number : ticket.matchSet) ticketGroups.get(number).add(ticket);
        }

        return isSuccess;
    }

    private void useTicket(Ticket ticket, boolean deleteFromGlobalQueue) {

        // remove from all match buckets this ticket belongs to
        for (int m : ticket.matchSet) {
            List<Ticket> list = ticketGroups.get(m);
            if (list != null) list.remove(ticket);
        }
        // selected ticket already polled; others need removal from global queue
        // remove safely (ConcurrentLinkedQueue supports remove)
        if(deleteFromGlobalQueue) globalQueue.remove(ticket);
    }

    public Result tryMatch() {
        Ticket ticket = globalQueue.poll();
        if (ticket == null) return Result.fail("No ticket available");

        Group bestGroup = null;
        for (int match : ticket.matchSet) {
            List<Ticket> neighborTickets = getNeighborTickets(match, ticket);
            Group group = buildGroup(neighborTickets, ticket);

            if (group != null && (bestGroup == null || group.getScore() > bestGroup.getScore())) {
                bestGroup = group;
            }
        }

        if (bestGroup != null){
            // 매칭된 모든 티켓을 큐/그룹에서 제거
            for (Ticket t : bestGroup.getTickets()) useTicket(t, !t.equals(ticket));

            return Result.success(bestGroup);
        } else {
            ticket.addCredit();
            globalQueue.offer(ticket);
            return Result.fail("No group available");
        }
    }

    public List<Ticket> getNeighborTickets(int match, Ticket selectedTicket) {
        List<Ticket> ticketGroup = ticketGroups.get(match);
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
            this.credit = 0;
            this.state = MatchingTicketState.CREATED;
        }

        public final UUID id;
        public final String userId;
        public final long timestamp;
        public final int rating;
        public final Set<Integer> matchSet;
        public MatchingTicketState state;
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
