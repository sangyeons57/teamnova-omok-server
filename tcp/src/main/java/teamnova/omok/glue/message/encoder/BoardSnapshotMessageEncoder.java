package teamnova.omok.glue.message.encoder;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionAccess;
import teamnova.omok.glue.game.session.model.messages.BoardSnapshotUpdate;

public final class BoardSnapshotMessageEncoder {
    private BoardSnapshotMessageEncoder() { }

    public static byte[] encode(GameSessionAccess session, BoardSnapshotUpdate update) {
        byte[] snapshot = update.snapshot();
        String encodedCells = Base64.getEncoder().encodeToString(snapshot);
        StringBuilder sb = new StringBuilder(encodedCells.length() + 128);
        sb.append('{')
          .append("\"sessionId\":\"").append(session.sessionId().asUuid()).append('\"')
          .append(',')
          .append("\"updatedAt\":").append(update.updatedAt())
          .append(',')
          .append("\"board\":{")
          .append("\"width\":").append(session.width()).append(',')
          .append("\"height\":").append(session.height()).append(',')
          .append("\"cells\":\"").append(encodedCells).append('\"')
          .append('}')
          .append('}');
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
}
