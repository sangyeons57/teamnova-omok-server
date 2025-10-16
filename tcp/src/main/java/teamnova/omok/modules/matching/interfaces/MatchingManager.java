package teamnova.omok.modules.matching.interfaces;

import teamnova.omok.modules.matching.models.MatchTicket;
import teamnova.omok.modules.matching.models.MatchResult;

/**
 * Minimal matching manager interface exposed by the module.
 * Keep it small and framework-agnostic.
 */
public interface MatchingManager {
    boolean enqueue(MatchTicket ticket);
    void cancel(String ticketId);
    MatchResult tryMatchOnce();
}
