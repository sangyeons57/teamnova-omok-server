package teamnova.omok.message.encoder;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import teamnova.omok.service.dto.BoardSnapshotUpdate;
import teamnova.omok.store.BoardStore;
import teamnova.omok.store.GameSession;

public final class BoardSnapshotMessageEncoder {
    private BoardSnapshotMessageEncoder() { }

    public static byte[] encode(BoardSnapshotUpdate update) {
        GameSession session = update.session();
        BoardStore boardStore = session.getBoardStore();
        byte[] snapshot = update.snapshot();
        String encodedCells = Base64.getEncoder().encodeToString(snapshot);
        StringBuilder sb = new StringBuilder(encodedCells.length() + 128);
        sb.append('{')
          .append("\"sessionId\":\"").append(session.getId()).append('\"')
          .append(',')
          .append("\"updatedAt\":").append(update.updatedAt())
          .append(',')
          .append("\"board\":{")
          .append("\"width\":").append(boardStore.width()).append(',')
          .append("\"height\":").append(boardStore.height()).append(',')
          .append("\"cells\":\"").append(encodedCells).append('\"')
          .append('}')
          .append('}');
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
}
