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
}
