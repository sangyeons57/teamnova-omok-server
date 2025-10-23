package teamnova.omok.glue.message.encoder;

import java.nio.charset.StandardCharsets;

import teamnova.omok.glue.game.session.interfaces.session.GameSessionAccess;
import teamnova.omok.glue.game.session.model.dto.TurnSnapshot;

public final class TurnStartedMessageEncoder {
    private TurnStartedMessageEncoder() { }

    public static byte[] encode(GameSessionAccess session, TurnSnapshot snapshot) {
        StringBuilder sb = new StringBuilder(192);
        sb.append('{')
          .append("\"sessionId\":\"").append(session.sessionId().asUuid()).append('\"')
          .append(',')
          .append("\"turn\":");

        if (snapshot == null) {
            sb.append("null");
        } else {
            sb.append('{')
                    .append("\"number\":").append(snapshot.turnNumber())
                    .append(',')
                    .append("\"round\":").append(snapshot.roundNumber())
                    .append(',')
                    .append("\"position\":").append(snapshot.positionInRound())
                    .append(',')
                    .append("\"playerIndex\":").append(snapshot.currentPlayerIndex())
                    .append(',')
                    .append("\"currentPlayerId\":");
            if (snapshot.currentPlayerId() == null) {
                sb.append("null");
            } else {
                sb.append('\"').append(MessageEncodingUtil.escape(snapshot.currentPlayerId())).append('\"');
            }
            sb.append(',')
                    .append("\"startAt\":").append(snapshot.turnStartAt())
                    .append(',')
                    .append("\"endAt\":").append(snapshot.turnEndAt())
                    .append('}');

        }
        sb.append('}');
        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }
}
