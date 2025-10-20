package teamnova.omok.modules.matching;

import java.util.Objects;

import teamnova.omok.modules.matching.interfaces.MatchingService;
import teamnova.omok.modules.matching.models.MatchResult;
import teamnova.omok.modules.matching.models.MatchTicket;
import teamnova.omok.modules.matching.models.MatchingConfig;
import teamnova.omok.modules.matching.services.DefaultMatchingService;

/**
 * Single entry point for the matching module.
 * Follows the same pattern as other modules' gateways.
 */
public final class MatchingGateway {
    private MatchingGateway() {}

    public static Handle open() {
        return new Handle(new DefaultMatchingService());
    }

    public static Handle open(MatchingConfig cfg) {
        return new Handle(new DefaultMatchingService(Objects.requireNonNull(cfg, "cfg")));
    }

    public static Handle wrap(MatchingService mainService) {
        return new Handle(mainService);
    }

    public static final class Handle {
        private final MatchingService delegate;

        private Handle(MatchingService delegate) {
            this.delegate = Objects.requireNonNull(delegate, "delegate");
        }

        public void enqueue(MatchTicket ticket) {
            delegate.enqueue(Objects.requireNonNull(ticket, "ticket"));
        }

        public void cancel(String ticketId) {
            delegate.cancel(ticketId);
        }

        public MatchResult tryMatchOnce() {
            return delegate.tryMatchOnce();
        }
    }
}
