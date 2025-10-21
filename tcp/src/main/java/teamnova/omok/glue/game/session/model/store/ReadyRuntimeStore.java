package teamnova.omok.glue.game.session.model.store;

import teamnova.omok.glue.game.session.model.result.ReadyResult;

/**
 * Holds temporary READY results emitted during lobby/PostGame flows.
 */
public final class ReadyRuntimeStore {
    private ReadyResult pendingReadyResult;

    public ReadyResult pendingReadyResult() {
        return pendingReadyResult;
    }

    public void pendingReadyResult(ReadyResult result) {
        this.pendingReadyResult = result;
    }

    public void clearPendingReadyResult() {
        this.pendingReadyResult = null;
    }
}
