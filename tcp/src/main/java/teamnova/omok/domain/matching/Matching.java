package teamnova.omok.domain.matching;

import teamnova.omok.domain.matching.model.Ticket;

import java.util.*;

public class Matching {
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

    public Matching() {
        this.globalQueue = new java.util.concurrent.ConcurrentLinkedDeque<>();
        this.ticketGroups = new HashMap<>();
    }

    public Ticket poll() {
        return globalQueue.poll();
    }

    public void createGroup (int k) {
        this.ticketGroups.put(k, new ArrayList<>());
    }

    public boolean enqueue(Ticket ticket) {
        boolean isSuccess = globalQueue.offer(ticket);

        if (isSuccess){
            for (int number : ticket.matchSet) ticketGroups.get(number).add(ticket);
            System.out.println("[MATCH][ENQ] user=" + ticket.id + " rating=" + ticket.rating + " modes=" + ticket.matchSet + " q=" + globalQueue.size());
        }

        return isSuccess;
    }

    public void dequeue(Ticket ticket) {
        if (ticket == null) return ;
        // Remove from global queue
        globalQueue.removeIf(t -> ticket.id.equals(t.id));
        // Remove from group buckets
        for (Map.Entry<Integer, List<Ticket>> e : ticketGroups.entrySet()) {
            e.getValue().removeIf(t -> ticket.id.equals(t.id));
        }
    }

    public void dequeue(String ticketId) {
        if (ticketId == null) return;
        // Remove from global queue
        globalQueue.removeIf(t -> ticketId.equals(t.id));
        // Remove from group buckets
        for (Map.Entry<Integer, List<Ticket>> e : ticketGroups.entrySet()) {
            e.getValue().removeIf(t -> ticketId.equals(t.id));
        }
    }

    public void offerOnlyGlobalQueue(Ticket ticket) {
        globalQueue.offer(ticket);
    }

    public List<Ticket> getPool(int match) {
        return ticketGroups.get(match);
    }


}