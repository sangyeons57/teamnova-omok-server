package teamnova.omok.modules.matching.services;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;

import teamnova.omok.modules.matching.interfaces.MatchingManager;
import teamnova.omok.modules.matching.models.*;

/**
 * DefaultMatchingManager now owns the full matching logic (no glue service dependency).
 */
public final class DefaultMatchingManager implements MatchingManager {
    // Configurable parameters
    private final MatchingConfig cfg;

    private final Deque<TicketInfo> globalQueue = new ConcurrentLinkedDeque<>();
    private final Map<Integer, List<TicketInfo>> ticketGroups = new HashMap<>();

    public DefaultMatchingManager() {
        this(MatchingConfig.defaults());
    }

    public DefaultMatchingManager(MatchingConfig cfg) {
        this.cfg = Objects.requireNonNull(cfg, "cfg");
        ticketGroups.put(2, new ArrayList<>());
        ticketGroups.put(3, new ArrayList<>());
        ticketGroups.put(4, new ArrayList<>());
    }

    @Override
    public boolean enqueue(MatchTicket ticket) {
        Objects.requireNonNull(ticket, "ticket");
        TicketInfo t = TicketInfo.create(ticket);
        boolean ok = globalQueue.offer(t);
        if (ok) {
            for (int m : t.getMatchSet()) ticketGroups.get(m).add(t);
            System.out.println("[MATCH][ENQ] user=" + t.getId() + " rating=" + t.getRating() + " modes=" + t.getMatchSet() + " q=" + globalQueue.size());
        }
        return ok;
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
        System.out.println("[MATCH][TRY] user=" + ticketInfo.getId() + " rating=" + ticketInfo.getRating() + " modes=" + ticketInfo.getMatchSet() + " qRemain=" + globalQueue.size());

        MatchGroup bestGroup = null;
        for (int match : ticketInfo.getMatchSet()) {
            List<TicketInfo> neighborTicketInfos = getNeighborTickets(match, ticketInfo);
            MatchGroup group = buildGroup(neighborTicketInfos);
            if (group != null && qualityCheck(group, bestGroup)) {
                bestGroup = group;
            }
        }

        if (bestGroup != null) {
            // remove matched tickets from all structures
            Set<String> ids = new HashSet<>();
            for (MatchTicket mt : bestGroup.tickets()) ids.add(mt.id());
            // remove selected ticket left out of queue already; remove others explicitly
            for (TicketInfo t : new ArrayList<>(globalQueue)) {
                if (ids.contains(t.getId())) globalQueue.remove(t);
            }
            for (List<TicketInfo> list : ticketGroups.values()) {
                list.removeIf(t -> ids.contains(t.getId()));
            }

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < bestGroup.tickets().size(); i++) {
                if (i > 0) sb.append(',');
                sb.append(bestGroup.tickets().get(i).id());
            }
            System.out.println("[MATCH][SUCCESS] size=" + bestGroup.tickets().size() + " users=[" + sb + "] score=" + bestGroup.score());
            return MatchResult.success(bestGroup);
        } else {
            ticketInfo.addCredit();
            System.out.println("[MATCH][REQUEUE] user=" + ticketInfo.getId() + " credit=" + ticketInfo.getCredit() + " reason=No group available");
            globalQueue.offer(ticketInfo);
            return MatchResult.fail("No group available");
        }
    }

    private List<TicketInfo> getNeighborTickets(int match, TicketInfo selectedTicketInfo) {
        List<TicketInfo> pool = ticketGroups.get(match);
        if (pool == null || pool.size() < match) return null;

        long waitSec = (System.currentTimeMillis() - selectedTicketInfo.getTimestamp()) / 1000;
        int timeTerm = (int) (cfg.timeMatchingWeight * Math.sqrt(Math.max(0, waitSec)));
        int windowMu = cfg.baseMatchingGap
            + timeTerm
            + cfg.creditMatchingWeight * selectedTicketInfo.getCredit();

        List<TicketInfo> candidates = new ArrayList<>(pool.size());
        for (TicketInfo t : pool) {
            if (!t.getId().equals(selectedTicketInfo.getId()) && Math.abs(t.getRating() - selectedTicketInfo.getRating()) <= windowMu) {
                candidates.add(t);
            }
        }
        if (candidates.size() < match - 1) {
            System.out.println("[MATCH][WINDOW] user=" + selectedTicketInfo.getId() + " match=" + match + " mu=" + windowMu + " candidates=" + candidates.size() + " -> insufficient");
            return null;
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
        System.out.println("[MATCH][WINDOW] user=" + selectedTicketInfo.getId() + " match=" + match + " mu=" + windowMu + " candidates=" + candidates.size() + " selectedSize=" + result.size());
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
        double devSum = 0.0;
        for (TicketInfo t : group) {
            double diff = t.getRating() - average;
            devSum += diff * diff;
        }
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
