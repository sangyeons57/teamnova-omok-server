package teamnova.omok.glue.game.session.interfaces.session;

import java.util.List;

import teamnova.omok.glue.game.session.model.dto.TurnSnapshot;
import teamnova.omok.glue.game.session.model.result.ReadyResult;
import teamnova.omok.glue.game.session.model.runtime.TurnPersonalFrame;

public interface GameSessionTurnRuntimeAccess {
    ReadyResult getPendingReadyResult();

    void setPendingReadyResult(ReadyResult result);

    void clearPendingReadyResult();

    void setPendingTimeoutFrame(TurnPersonalFrame frame);

    TurnPersonalFrame consumePendingTimeoutFrame();

    TurnSnapshot getPendingTurnSnapshot();

    void setPendingTurnSnapshot(TurnSnapshot snapshot, long occurredAtMillis);

    void clearPendingTurnSnapshot();

    void resetPersonalTurnFrames();

    TurnPersonalFrame beginPersonalTurnFrame(TurnSnapshot snapshot,
                                             long startedAt);

    TurnPersonalFrame currentPersonalTurnFrame();

    List<TurnPersonalFrame> personalTurnFrames();

}
