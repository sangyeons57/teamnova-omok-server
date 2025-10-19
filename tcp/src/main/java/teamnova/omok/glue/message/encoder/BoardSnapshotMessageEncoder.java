package teamnova.omok.glue.message.encoder;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionBoardAccess;
import teamnova.omok.glue.game.session.interfaces.session.GameSessionParticipantsAccess;
import teamnova.omok.glue.game.session.model.messages.BoardSnapshotUpdate;

public final class BoardSnapshotMessageEncoder {
    private BoardSnapshotMessageEncoder() { }

    public static byte[] encode(BoardSnapshotUpdate update) {
        GameSessionBoardAccess boardAccess = update.getBoardAccess();
        byte[] snapshot = update.snapshot();
        String encodedCells = Base64.getEncoder().encodeToString(snapshot);
        StringBuilder sb = new StringBuilder(encodedCells.length() + 128);
        sb.append('{')
          .append("\"sessionId\":\"").append(boardAccess.sessionId().asUuid()).append('\"')
          .append(',')
          .append("\"updatedAt\":").append(update.updatedAt())
          .append(',')
          .append("\"board\":{")
          .append("\"width\":").append(boardAccess.width()).append(',')
          .append("\"height\":").append(boardAccess.height()).append(',')
          .append("\"cells\":\"").append(encodedCells).append('\"')
          .append('}')
          .append('}');
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
}
