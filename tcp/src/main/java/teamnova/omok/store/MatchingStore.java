package teamnova.omok.store;

import teamnova.omok.domain.matching.Matching;

public class MatchingStore {
    private final Matching matching;

    public MatchingStore(Matching matching) {
        this.matching = matching;
    }

    public Matching getMatching() {
        return matching;
    }
}


