package teamnova.omok.modules.matching;

import java.util.Objects;

import teamnova.omok.modules.matching.interfaces.MatchingManager;
import teamnova.omok.modules.matching.models.MatchResult;
import teamnova.omok.modules.matching.models.MatchTicket;
import teamnova.omok.modules.matching.models.MatchingConfig;
import teamnova.omok.modules.matching.services.DefaultMatchingManager;

/**
 * Single entry point for the matching module.
 * Follows the same pattern as other modules' gateways.
 */
public final class MatchingGateway {
    private MatchingGateway() {}

    public static Handle open() {
        return new Handle(new DefaultMatchingManager());
    }

    public static Handle open(MatchingConfig cfg) {
        return new Handle(new DefaultMatchingManager(Objects.requireNonNull(cfg, "cfg")));
    }

    public static Handle wrap(MatchingManager manager) {
        return new Handle(manager);
    }

    public static final class Handle {
        private final MatchingManager delegate;

        private Handle(MatchingManager delegate) {
            this.delegate = Objects.requireNonNull(delegate, "delegate");
        }

        public boolean enqueue(MatchTicket ticket) {
            return delegate.enqueue(Objects.requireNonNull(ticket, "ticket"));
        }

        public void cancel(String ticketId) {
            delegate.cancel(ticketId);
        }

        public MatchResult tryMatchOnce() {
            return delegate.tryMatchOnce();
        }
    }
}
