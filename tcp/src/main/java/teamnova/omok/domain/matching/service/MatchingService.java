package teamnova.omok.domain.matching.service;

import teamnova.omok.domain.matching.Matching;
import teamnova.omok.domain.matching.model.Group;
import teamnova.omok.domain.matching.model.MatchingResult;
import teamnova.omok.domain.matching.model.Ticket;

import java.util.*;

public class MatchingService {

    public MatchingResult tryMatch(Matching matching) {
        Ticket ticket = matching.poll();
        if (ticket == null) return MatchingResult.fail("No ticket available");
        System.out.println("[MATCH][TRY] user=" + ticket.id + " rating=" + ticket.rating + " modes=" + ticket.matchSet);

        Group bestGroup = null;
        for (int match : ticket.matchSet) {
            Group group = buildGroup(getNeighborTickets(matching, match, ticket));

            if (group != null && qualityCheck(group, bestGroup)) {
                bestGroup = group;
            }
        }

        if (bestGroup != null){
            // 매칭된 모든 티켓을 큐/그룹에서 제거
            for (Ticket t : bestGroup.getTickets()) matching.dequeue(t);

            logGroup(bestGroup);
            return MatchingResult.success(bestGroup);
        } else {
            ticket.addCredit();
            System.out.println("[MATCH][REQUEUE] user=" + ticket.id + " credit=" + ticket.getCredit() + " reason=No group available");
            matching.offerOnlyGlobalQueue(ticket);
            return MatchingResult.fail("No group available");
        }
    }

    private List<Ticket> getNeighborTickets(Matching matching, int match, Ticket selectedTicket) {
        List<Ticket> pool = matching.getPool(match);
        if (pool == null || pool.size() < match) return null;

        long waitSec = (System.currentTimeMillis() - selectedTicket.timestamp) / 1000;
        int windowMu = windowMu( waitSec, selectedTicket);

        // Build a candidate list excluding the selected ticket
        List<Ticket> candidates = new ArrayList<>(pool.size());
        for (Ticket t : pool) {
            if (!t.id.equals(selectedTicket.id) && differenceCheck(t.rating, selectedTicket.rating, windowMu)) {
                candidates.add(t);
            }
        }

        if (candidates.size() < match -1) {
            System.out.println("[MATCH][WINDOW] user=" + selectedTicket.id + " match=" + match + " mu=" + windowMu + " candidates=" + candidates.size() + " -> insufficient");
            return null;
        }

        sortTickets(candidates, selectedTicket);

        List<Ticket> result = new ArrayList<>(match);
        result.add(selectedTicket);
        for (int i = 0; i < candidates.size() && result.size() < match; i++) result.add(candidates.get(i));
        System.out.println("[MATCH][WINDOW] user=" + selectedTicket.id + " match=" + match + " mu=" + windowMu + " candidates=" + candidates.size() + " selectedSize=" + result.size());
        return result;
    }

    private int windowMu(long waitSec, Ticket selectedTicket) {
        return Matching.BASE_MATCHING_GAP
                + (int)(Matching.TIME_MATCHING_WEIGHT * (waitSec/Matching.TIME_MATCHING_WEIGHT_PER_SEC))
                + Matching.CREDIT_MATCHING_WEIGHT * selectedTicket.getCredit();

        // Build candidate list excluding the selected ticket
    }

    private boolean differenceCheck(int a, int b, int limit) {
        return Math.abs(a - b) <= limit;
    }

    // Sort by: rating proximity ASC, credit DESC, timestamp ASC
    private void sortTickets(List<Ticket> tickets, Ticket selectedTicket) {
        tickets.sort(new Comparator<Ticket>() {
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
    }

    public Group buildGroup(List<Ticket> group) {
        if (group == null || group.size() < 2) return null;

        // If we couldn't collect enough neighbors to satisfy the intended match size, skip
        int matchSize = group.size();

        int[] mus = group.stream().mapToInt(t -> t.rating).toArray();
        double mean = Arrays.stream(mus).average().orElse(0.0);
        double std = Math.sqrt(Arrays.stream(mus).mapToDouble(m -> Math.pow(m - mean, 2)).average().orElse(0.0));
        int maxDelta = Arrays.stream(mus).map(x->(int)Math.abs(x - mean)).max().orElse(0);
        double meanCredit = group.stream().mapToInt(Ticket::getCredit).average().orElse(0.0);


        int score = (int)Math.round(Matching.GROUP_BASE_SCORE
                + Matching.GROUP_STANDARD_DEVIATION_NEGATIVE_WEIGHT * std
                + Matching.GROUP_MAX_DELTA_NEGATIVE_WEIGHT * maxDelta
                + Matching.GROUP_CREDIT_POSITIVE_WEIGHT * meanCredit
                + Matching.GROUP_HEADCOUNT_POSITIVE_WEIGHT * matchSize);

        return new Group(group, score);
    }

    private boolean qualityCheck(Group currentGroup, Group bestGroup) {
        if (Matching.LIMIT_GROUP_SCORE > currentGroup.getScore())
            return false;

        if (bestGroup == null)
            return true;
        else
            return (currentGroup.getScore() > bestGroup.getScore());
    }

    private void logGroup(Group group) {
        // build concise user list for log
        StringBuilder ids = new StringBuilder();
        for (int i = 0; i < group.getTickets().size(); i++) {
            if (i > 0) ids.append(',');
            ids.append(group.getTickets().get(i).id);
        }
        System.out.println("[MATCH][SUCCESS] size=" + group.getTickets().size() + " users=[" + ids + "] score=" + group.getScore());
    }
}
