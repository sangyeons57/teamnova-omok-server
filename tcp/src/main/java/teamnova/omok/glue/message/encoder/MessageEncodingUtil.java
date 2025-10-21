package teamnova.omok.glue.message.encoder;

import teamnova.omok.glue.game.session.interfaces.GameTurnService;
import teamnova.omok.glue.game.session.model.dto.TurnSnapshot;

final class MessageEncodingUtil {
    private MessageEncodingUtil() {}

    static String escape(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder(input.length() + 16);
        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);
            if (ch == '"' || ch == '\\') {
                sb.append('\\').append(ch);
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    static void appendTurn(StringBuilder sb, TurnSnapshot turn) {
        if (turn == null) {
            sb.append("null");
            return;
        }
        sb.append('{')
          .append("\"number\":").append(turn.turnNumber())
          .append(',')
          .append("\"round\":").append(turn.roundNumber())
          .append(',')
          .append("\"position\":").append(turn.positionInRound())
          .append(',')
          .append("\"playerIndex\":").append(turn.currentPlayerIndex())
          .append(',')
          .append("\"currentPlayerId\":");
        if (turn.currentPlayerId() == null) {
            sb.append("null");
        } else {
            sb.append('\"').append(escape(turn.currentPlayerId())).append('\"');
        }
        sb.append(',')
          .append("\"startAt\":").append(turn.turnStartAt())
          .append(',')
          .append("\"endAt\":").append(turn.turnEndAt())
          .append('}');
    }
}
