package teamnova.omok.service;

import java.util.*;

public class MatchingService {
    private final Queue<Ticket> globalQueue;
    private final HashMap<Integer, List<Ticket>> ticketGroups;

    public static int BASE_MATCHING_GAP = 100;
    public static float TIME_MATCHING_WEIGHT_PER_SEC = 5.0f;
    public static int TIME_MATCHING_WEIGHT = 50;
    public static int CREDIT_MATCHING_WEIGHT = 50;

    public static final int GROUP_BASE_SCORE = 10_000;
    public static final int GROUP_STANDARD_DEVIATION_NEGATIVE_WEIGHT = -100;
    public static final int GROUP_MAX_DELTA_NEGATIVE_WEIGHT = -2;
    public static final int GROUP_CREDIT_POSITIVE_WEIGHT = 25;
    public static final int GROUP_HEADCOUNT_POSITIVE_WEIGHT = 50;

    public static final int LIMIT_GROUP_SCORE = 10_000;

    public MatchingService() {
        this.globalQueue = new java.util.concurrent.ConcurrentLinkedDeque<>();
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

    /**
     * Cancel and remove a ticket from all queues/buckets by its id.
     */
    public void cancel(String ticketId) {
        if (ticketId == null) return;
        // Remove from global queue
        globalQueue.removeIf(t -> ticketId.equals(t.id));
        // Remove from group buckets
        for (Map.Entry<Integer, List<Ticket>> e : ticketGroups.entrySet()) {
            e.getValue().removeIf(t -> ticketId.equals(t.id));
        }
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

            if (group != null && qualityCheck(group, bestGroup)) {
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
        List<Ticket> pool = ticketGroups.get(match);
        if (pool == null || pool.size() < match) return null;

        long waitSec = (System.currentTimeMillis() - selectedTicket.timestamp) / 1000;
        int windowMu = BASE_MATCHING_GAP
                + (int)(TIME_MATCHING_WEIGHT * (waitSec/TIME_MATCHING_WEIGHT_PER_SEC))
                + CREDIT_MATCHING_WEIGHT * selectedTicket.getCredit();

        // Build candidate list excluding the selected ticket
        List<Ticket> candidates = new ArrayList<>(pool.size());
        for (Ticket t : pool) {
            if (!t.id.equals(selectedTicket.id) && Math.abs(t.rating - selectedTicket.rating) <= windowMu) {
                candidates.add(t);
            }
        }

        if (candidates.size() < match -1) return null;

        // Sort by: rating proximity ASC, credit DESC, timestamp ASC
        candidates.sort(new Comparator<Ticket>() {
            @Override
            public int compare(Ticket a, Ticket b) {
                int da = Math.abs(a.rating - selectedTicket.rating);
                int db = Math.abs(b.rating - selectedTicket.rating);
                if (da != db) return Integer.compare(da, db);
                long wa = a.timestamp, wb = b.timestamp;
                if(wa!=wb) return Long.compare(wa, wb);
                return Integer.compare(b.getCredit(), a.getCredit());
            }
        });

        List<Ticket> result = new ArrayList<>(match);
        result.add(selectedTicket);
        for (int i = 0; i < candidates.size() && result.size() < match; i++) result.add(candidates.get(i));
        return result;
    }

    public Group buildGroup(List<Ticket> group, Ticket selectedTicket) {
        if (group == null || group.size() < 2) return null;

        // If we couldn't collect enough neighbors to satisfy the intended match size, skip
        int matchSize = group.size();

        int[] mus = group.stream().mapToInt(t -> t.rating).toArray();
        double mean = Arrays.stream(mus).average().orElse(0.0);
        double std = Math.sqrt(Arrays.stream(mus).mapToDouble(m -> Math.pow(m - mean, 2)).average().orElse(0.0));
        int maxDelta = Arrays.stream(mus).map(x->(int)Math.abs(x - mean)).max().orElse(0);
        double meanCredit = group.stream().mapToInt(Ticket::getCredit).average().orElse(0.0);


        int score = (int)Math.round(GROUP_BASE_SCORE
                + GROUP_STANDARD_DEVIATION_NEGATIVE_WEIGHT * std
                + GROUP_MAX_DELTA_NEGATIVE_WEIGHT * maxDelta
                + GROUP_CREDIT_POSITIVE_WEIGHT * meanCredit
                + GROUP_HEADCOUNT_POSITIVE_WEIGHT * matchSize);

        return new Group(group, score);
    }

    public boolean qualityCheck(Group currentGroup, Group bestGroup) {
        if (LIMIT_GROUP_SCORE > currentGroup.getScore())
            return false;

        if (bestGroup == null)
            return true;
        else
            return (currentGroup.getScore() > bestGroup.getScore());
    }

    public static class Ticket {
        public Ticket(String userId, int rating, Set<Integer> matchSet) {
            this.id = userId;
            this.timestamp = System.currentTimeMillis();
            this.rating = rating;
            this.matchSet = matchSet;
            this.credit = 0;
            this.state = MatchingTicketState.CREATED;
        }

        public final String id;
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
