package teamnova.omok.message.encoder;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import teamnova.omok.service.dto.BoardSnapshotUpdate;
import teamnova.omok.domain.session.game.GameSession;
import teamnova.omok.domain.session.game.entity.board.BoardReadable;

public final class BoardSnapshotMessageEncoder {
    private BoardSnapshotMessageEncoder() { }

    public static byte[] encode(BoardSnapshotUpdate update) {
        GameSession session = update.session();
        BoardReadable board = session.getBoard();
        byte[] snapshot = update.snapshot();
        String encodedCells = Base64.getEncoder().encodeToString(snapshot);
        StringBuilder sb = new StringBuilder(encodedCells.length() + 128);
        sb.append('{')
          .append("\"sessionId\":\"").append(session.getId()).append('\"')
          .append(',')
          .append("\"updatedAt\":").append(update.updatedAt())
          .append(',')
          .append("\"board\":{")
          .append("\"width\":").append(board.width()).append(',')
          .append("\"height\":").append(board.height()).append(',')
          .append("\"cells\":\"").append(encodedCells).append('\"')
          .append('}')
          .append('}');
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
}
