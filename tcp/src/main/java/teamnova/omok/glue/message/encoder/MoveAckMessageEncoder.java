package teamnova.omok.glue.message.encoder;

import java.nio.charset.StandardCharsets;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionAccess;
import teamnova.omok.glue.game.session.model.result.MoveStatus;
import teamnova.omok.glue.game.session.model.runtime.TurnPersonalFrame;

public final class MoveAckMessageEncoder {
    private MoveAckMessageEncoder() {}

    public static byte[] encode(GameSessionAccess session, TurnPersonalFrame frame) {
        // Per client spec, MOVE ACK must be minimal: {"status":"<status_label>"}
        // We map MoveStatus.SUCCESS -> OK, otherwise -> ERROR. If frame is null (ignored), treat as ERROR.
        String statusLabel;
        if (frame != null && frame.outcomeStatus() == MoveStatus.SUCCESS) {
            statusLabel = "OK";
        } else {
            statusLabel = "ERROR";
        }
        String json = "{\"status\":\"" + statusLabel + "\"}";
        return json.getBytes(StandardCharsets.UTF_8);
    }
}
