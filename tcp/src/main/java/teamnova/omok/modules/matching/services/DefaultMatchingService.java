package teamnova.omok.modules.matching.services;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArrayList;

import teamnova.omok.modules.matching.interfaces.MatchingService;
import teamnova.omok.modules.matching.models.*;

/**
 * DefaultMatchingManager now owns the full matching logic (no glue service dependency).
 */
public final class DefaultMatchingService implements MatchingService {
    // Configurable parameters
    private final MatchingConfig cfg;

    private final Deque<TicketInfo> globalQueue = new ConcurrentLinkedDeque<>();
    private final Map<Integer, List<TicketInfo>> ticketGroups = new HashMap<>();

    public DefaultMatchingService() {
        this(MatchingConfig.defaults());
    }

    public DefaultMatchingService(MatchingConfig cfg) {
        this.cfg = Objects.requireNonNull(cfg, "cfg");
        ticketGroups.put(2, new CopyOnWriteArrayList<>());
        ticketGroups.put(3, new CopyOnWriteArrayList<>());
        ticketGroups.put(4, new CopyOnWriteArrayList<>());
    }

    @Override
    public void enqueue(MatchTicket ticket) {
        Objects.requireNonNull(ticket, "ticket");
        TicketInfo t = TicketInfo.create(ticket);
        boolean ok = globalQueue.offer(t);
        if (ok) {
            for (int m : t.getMatchSet()) ticketGroups.get(m).add(t);
        }
    }

    @Override
    public void cancel(String ticketId) {
        if (ticketId == null) return;
        globalQueue.removeIf(t -> ticketId.equals(t.getId()));
        for (Map.Entry<Integer, List<TicketInfo>> e : ticketGroups.entrySet()) {
            e.getValue().removeIf(t -> ticketId.equals(t.getId()));
        }
    }

    @Override
    public MatchResult tryMatchOnce() {
        TicketInfo ticketInfo = globalQueue.poll();
        if (ticketInfo == null) return MatchResult.fail("No ticket available");

        boolean consumed = false;   // true when a success group is formed and ticket should NOT be returned
        boolean reoffered = false;  // true when we explicitly re-queued the ticket on failure path
        try {
            MatchGroup bestGroup = null;
            for (int match : ticketInfo.getMatchSet()) {
                List<TicketInfo> pool = ticketGroups.get(match);
                if (pool == null || pool.size() < match) continue;

                // Build neighbors using the current snapshot of the pool (CopyOnWriteArrayList safe)
                List<TicketInfo> neighborTicketInfos = getNeighborTickets(match, pool, ticketInfo);
                MatchGroup group = buildGroup(neighborTicketInfos);
                if (group != null && qualityCheck(group, bestGroup)) {
                    bestGroup = group;
                }
            }

            if (bestGroup != null) {
                // remove matched tickets from all structures
                for (MatchTicket mt : bestGroup.tickets()){
                    cancel(mt.id());
                }
                consumed = true; // do not return polled ticket on success
                return MatchResult.success(bestGroup);
            } else {
                ticketInfo.addCredit();
                globalQueue.offer(ticketInfo);
                reoffered = true;
                return MatchResult.fail("No group available");
            }
        } finally {
            // If an exception occurs before success or explicit re-offer, ensure the polled ticket is restored.
            if (!consumed && !reoffered) {
                // offer to the front to minimize starvation
                globalQueue.offerFirst(ticketInfo);
            }
        }
    }

    private List<TicketInfo> getNeighborTickets(int match, List<TicketInfo> pool, TicketInfo selectedTicketInfo) {

        long waitSec = (System.currentTimeMillis() - selectedTicketInfo.getTimestamp()) / 1000;
        int timeTerm = (int) (cfg.timeMatchingWeight * Math.max(0, waitSec));
        int creditTerm = (int) (cfg.creditMatchingWeight * selectedTicketInfo.getCredit());
        int windowMu = cfg.baseMatchingGap + timeTerm + creditTerm;

        List<TicketInfo> candidates = new ArrayList<>(pool.size());
        for (TicketInfo t : pool) {
            if (!t.getId().equals(selectedTicketInfo.getId()) && Math.abs(t.getRating() - selectedTicketInfo.getRating()) <= windowMu) {
                candidates.add(t);
            }
        }

        candidates.sort((a, b) -> {
            int da = Math.abs(a.getRating() - selectedTicketInfo.getRating());
            int db = Math.abs(b.getRating() - selectedTicketInfo.getRating());
            if (da != db) return Integer.compare(da, db);
            long wa = a.getTimestamp(), wb = b.getTimestamp();
            if (wa != wb) return Long.compare(wa, wb);
            return Integer.compare(b.getCredit(), a.getCredit());
        });

        List<TicketInfo> result = new ArrayList<>(match);
        result.add(selectedTicketInfo);
        for (int i = 0; i < candidates.size() && result.size() < match; i++) result.add(candidates.get(i));
        return result;
    }

    private MatchGroup buildGroup(List<TicketInfo> group) {
        if (group == null) return null;
        if (group.isEmpty()) return null;
        int minRating = Integer.MAX_VALUE;
        int maxRating = Integer.MIN_VALUE;
        int totalCredit = 0;
        int sum = 0;
        for (TicketInfo t : group) {
            sum += t.getRating();
            totalCredit += t.getCredit();
            if (t.getRating() < minRating) minRating = t.getRating();
            if (t.getRating() > maxRating) maxRating = t.getRating();
        }
        double average = (double) sum / group.size();
        double devSum = group.stream().map(t -> t.getRating() - average).map(diff -> diff * diff).reduce(0.0, Double::sum);
        int delta = (maxRating - minRating);
        int deltaPenalty = (delta > cfg.baseMatchingGap) ? (cfg.groupMaxDeltaNegativeWeight * delta) : 0;
        int score = cfg.groupBaseScore
            + (int) (cfg.groupStdDevNegativeWeight * Math.sqrt(devSum / group.size()))
            + deltaPenalty
            + cfg.groupCreditPositiveWeight * totalCredit
            + cfg.groupHeadcountPositiveWeight * group.size();

        if (score < cfg.limitGroupScore) return null;

        return new MatchGroup(group, score);
    }

    private boolean qualityCheck(MatchGroup currentGroup, MatchGroup bestGroup) {
        if (currentGroup == null) return false;
        if (bestGroup == null) return true;
        return currentGroup.score() > bestGroup.score();
    }
}
